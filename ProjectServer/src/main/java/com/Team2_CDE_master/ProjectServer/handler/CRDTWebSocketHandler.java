package com.Team2_CDE_master.ProjectServer.handler;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import com.Team2_CDE_master.ProjectServer.persistence.DocumentPersistenceService;
import com.Team2_CDE_master.ProjectServer.session.DocumentSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.*;

@Component
public class CRDTWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private DocumentPersistenceService persistenceService;

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, String>  sessionRoles   = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionSiteIds = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String docId = extractDocId(session);
        String role  = extractRole(session);

        rooms.computeIfAbsent(docId, id -> Collections.synchronizedSet(new HashSet<>()))
                .add(session);
        sessionRoles.put(session.getId(), role);

        if (DocumentSession.get(docId) == null) {
            BlockCRDT saved = persistenceService.loadDocument(docId);
            if (saved != null) {
                DocumentSession.seed(docId, saved);
                System.out.println("[WS] Auto-loaded '" + docId + "' from database");
            } else {
                DocumentSession.getOrCreate(docId);
            }
        }

        try {
            BlockCRDT doc = DocumentSession.get(docId);
            if (doc != null) {
                synchronized (doc) {
                    sendFullSync(session, doc);
                }
            }
        } catch (Exception e) {
            System.err.println("[WS] Failed to send full sync: " + e.getMessage());
        }

        System.out.println("[WS] Client connected — doc: " + docId + ", role: " + role
                + ", session: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String docId = extractDocId(session);
        Set<WebSocketSession> room = rooms.get(docId);
        if (room != null) room.remove(session);

        sessionRoles.remove(session.getId());
        sessionSiteIds.remove(session.getId());

        System.out.println("[WS] Client disconnected — doc: " + docId
                + ", status: " + status.getCode());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String docId = extractDocId(session);
        JSONObject json = new JSONObject(message.getPayload());
        String type = json.getString("type");

        if (type.equals("cursor_update") || type.equals("presence")) {
            broadcast(docId, message.getPayload(), session);
            return;
        }

        if (type.equals("document_deleted")) {
            broadcast(docId, message.getPayload(), session);
            return;
        }

        String role = sessionRoles.getOrDefault(session.getId(), "VIEWER");
        if ("VIEWER".equals(role)) {
            System.out.println("[WS][Security] Blocked '" + type
                    + "' from VIEWER " + session.getId());
            return;
        }

        BlockCRDT doc = DocumentSession.getOrCreate(docId);
        synchronized (doc) {
            applyOperation(doc, type, json);
        }

        broadcast(docId, message.getPayload(), session);
    }

    private void applyOperation(BlockCRDT doc, String type, JSONObject json) {
        switch (type) {
            case "insert_char"  -> applyInsertChar(doc, json);
            case "delete_char"  -> applyDeleteChar(doc, json);
            case "replace_char" -> applyReplaceChar(doc, json);
            case "formatting"   -> applyFormatting(doc, json);
            case "insert_block" -> applyInsertBlock(doc, json);
            case "delete_block" -> applyDeleteBlock(doc, json);
            case "split_block"  -> applySplitBlock(doc, json);
            case "merge_blocks" -> applyMergeBlocks(doc, json);
            default -> System.err.println("[WS] Unknown op type: " + type);
        }
    }

    private void applyInsertChar(BlockCRDT doc, JSONObject json) {
        Block block = findOrWarnBlock(doc, json.getString("blockId"));
        if (block == null) return;
        CharID charId   = parseCharID(json.getJSONObject("charId"));
        CharID parentId = json.isNull("parentId") ? null : parseCharID(json.getJSONObject("parentId"));
        char ch         = json.getString("char").charAt(0);
        CharNode node = new CharNode(charId, parentId, ch);
        node.setBold(json.optBoolean("bold", false));
        node.setItalic(json.optBoolean("italic", false));
        block.getContent().addChar(node);
    }

    private void applyDeleteChar(BlockCRDT doc, JSONObject json) {
        Block block = findOrWarnBlock(doc, json.getString("blockId"));
        if (block == null) return;
        CharID targetId = parseCharID(json.getJSONObject("charId"));
        new DeleteCharOperation(json.getString("blockId"), targetId).apply(block.getContent());
    }

    private void applyReplaceChar(BlockCRDT doc, JSONObject json) {
        Block block = findOrWarnBlock(doc, json.getString("blockId"));
        if (block == null) return;
        CharID oldId        = parseCharID(json.getJSONObject("oldCharId"));
        JSONObject newNodeJ = json.getJSONObject("newNode");
        CharID newId        = parseCharID(newNodeJ.getJSONObject("charId"));
        CharID newParent    = newNodeJ.isNull("parentId") ? null : parseCharID(newNodeJ.getJSONObject("parentId"));
        char ch             = newNodeJ.getString("char").charAt(0);
        new ReplaceCharOperation(json.getString("blockId"), oldId, new CharNode(newId, newParent, ch))
                .apply(block.getContent());
    }

    private void sendFullSync(WebSocketSession session, BlockCRDT doc) throws Exception {
        org.json.JSONArray blocks = new org.json.JSONArray();

        for (Block block : doc.allBlocks) {
            if (block.checkDeleted()) continue;

            org.json.JSONObject blockJson = new org.json.JSONObject();
            blockJson.put("siteId",  block.getMyId().siteId);
            blockJson.put("counter", block.getMyId().counter);

            if (block.getParentId() != null) {
                org.json.JSONObject parentJson = new org.json.JSONObject();
                parentJson.put("siteId",  block.getParentId().siteId);
                parentJson.put("counter", block.getParentId().counter);
                blockJson.put("parentId", parentJson);
            } else {
                blockJson.put("parentId", org.json.JSONObject.NULL);
            }

            org.json.JSONArray chars = new org.json.JSONArray();
            for (CharNode node : block.getContent().allNodes) {
                org.json.JSONObject charJson = new org.json.JSONObject();
                charJson.put("siteId", node.getMyId().siteId);
                charJson.put("myNum",  node.getMyId().myNum);

                if (node.getParentId() != null) {
                    org.json.JSONObject cp = new org.json.JSONObject();
                    cp.put("siteId", node.getParentId().siteId);
                    cp.put("myNum",  node.getParentId().myNum);
                    charJson.put("parentId", cp);
                } else {
                    charJson.put("parentId", org.json.JSONObject.NULL);
                }

                charJson.put("char",    String.valueOf(node.getMyChar()));
                charJson.put("deleted", node.checkDeleted());
                charJson.put("bold",    node.checkBold());
                charJson.put("italic",  node.checkItalic());
                chars.put(charJson);
            }
            blockJson.put("chars", chars);
            blocks.put(blockJson);
        }

        org.json.JSONObject msg = new org.json.JSONObject();
        msg.put("type",   "full_sync");
        msg.put("blocks", blocks);

        session.sendMessage(new TextMessage(msg.toString()));
    }

    private void applyFormatting(BlockCRDT doc, JSONObject json) {
        Block block = findOrWarnBlock(doc, json.getString("blockId"));
        if (block == null) return;
        new FormattingOperation(
                parseCharID(json.getJSONObject("charId")),
                json.getString("formatType"),
                json.getBoolean("value")
        ).apply(block.getContent());
    }

    private void applyInsertBlock(BlockCRDT doc, JSONObject json) {
        BlockID blockId  = parseBlockID(json.getJSONObject("blockId"));
        BlockID parentId = json.isNull("parentBlockId") ? null
                : parseBlockID(json.getJSONObject("parentBlockId"));
        doc.addBlock(new Block(blockId, parentId));
    }

    private void applyDeleteBlock(BlockCRDT doc, JSONObject json) {
        doc.deleteBlock(parseBlockID(json.getJSONObject("blockId")));
    }

    private void applySplitBlock(BlockCRDT doc, JSONObject json) {
        doc.splitBlock(
                parseBlockID(json.getJSONObject("targetBlockId")),
                json.getInt("splitIndex"),
                parseBlockID(json.getJSONObject("newBlockId")));
    }

    private void applyMergeBlocks(BlockCRDT doc, JSONObject json) {
        doc.mergeBlocks(
                parseBlockID(json.getJSONObject("firstBlockId")),
                parseBlockID(json.getJSONObject("secondBlockId")));
    }

    private void broadcast(String docId, String payload, WebSocketSession sender) throws Exception {
        Set<WebSocketSession> room = rooms.get(docId);
        if (room == null) return;
        for (WebSocketSession s : room) {
            if (s.isOpen() && !s.getId().equals(sender.getId())) {
                s.sendMessage(new TextMessage(payload));
            }
        }
    }

    private CharID parseCharID(JSONObject json) {
        return new CharID(json.getInt("siteId"), json.getInt("myNum"));
    }

    private BlockID parseBlockID(JSONObject json) {
        return new BlockID(json.getInt("siteId"), json.getInt("counter"));
    }

    private Block findOrWarnBlock(BlockCRDT doc, String blockIdStr) {
        String[] parts = blockIdStr.replace("B", "").split("_");
        BlockID id = new BlockID(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        Block block = doc.findBlock(id);
        if (block == null) System.err.println("[WS] Block not found: " + blockIdStr);
        return block;
    }

    private String extractDocId(WebSocketSession session) {
        String path = session.getUri().getPath();
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private String extractRole(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null || query.isBlank()) return "VIEWER";
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "role".equalsIgnoreCase(kv[0].trim())) {
                return kv[1].trim().equalsIgnoreCase("EDITOR") ? "EDITOR" : "VIEWER";
            }
        }
        return "VIEWER";
    }

    @Scheduled(fixedDelay = 30_000)
    public void autoSaveAll() {
        Map<String, BlockCRDT> sessions = DocumentSession.getAllSessions();
        if (sessions.isEmpty()) return;
        System.out.println("[AutoSave] Saving " + sessions.size() + " document(s)...");
        for (Map.Entry<String, BlockCRDT> entry : sessions.entrySet()) {
            String docId  = entry.getKey();
            BlockCRDT doc = entry.getValue();
            try {
                synchronized (doc) {
                    persistenceService.saveDocument(doc, docId, docId);
                }
            } catch (Exception e) {
                System.err.println("[AutoSave] Failed '" + docId + "': " + e.getMessage());
            }
        }
    }
}
