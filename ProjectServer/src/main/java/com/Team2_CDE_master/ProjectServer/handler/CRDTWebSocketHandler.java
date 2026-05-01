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

// @Component makes Spring manage this bean so @Autowired and @Scheduled work.
// WebSocketConfig now autowires this instead of calling "new CRDTWebSocketHandler()".
@Component
public class CRDTWebSocketHandler extends TextWebSocketHandler {

    // Spring injects the persistence service — handles saving/loading to H2
    @Autowired
    private DocumentPersistenceService persistenceService;

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String docId = extractDocId(session);
        rooms.computeIfAbsent(docId, id -> Collections.synchronizedSet(new HashSet<>())).add(session);

        // When the first client connects to a document, check the database.
        // If this document was saved before, restore it so the client sees the old content.
        if (DocumentSession.get(docId) == null) {
            BlockCRDT saved = persistenceService.loadDocument(docId);
            if (saved != null) {
                DocumentSession.seed(docId, saved);
                System.out.println("Auto-loaded '" + docId + "' from database");
            }
        }

        System.out.println("Client connected to doc: " + docId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String docId = extractDocId(session);

        JSONObject json = new JSONObject(message.getPayload());
        String type = json.getString("type");

        if (type.equals("cursor_update")) {
            broadcast(docId, message.getPayload(), session);
            return;
        }

        BlockCRDT doc = DocumentSession.getOrCreate(docId);

        synchronized (doc) {
            applyOperation(doc, type, json);
        }

        broadcast(docId, message.getPayload(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String docId = extractDocId(session);
        Set<WebSocketSession> room = rooms.get(docId);
        if (room != null) room.remove(session);
        System.out.println("Client disconnected from doc: " + docId);
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
            default -> System.err.println("Unknown op type: " + type);
        }
    }

    private void applyInsertChar(BlockCRDT doc, JSONObject json) {
        String blockId = json.getString("blockId");
        CharID charId = parseCharID(json.getJSONObject("charId"));
        CharID parentId = json.isNull("parentId") ? null : parseCharID(json.getJSONObject("parentId"));
        char ch = json.getString("char").charAt(0);

        Block block = findOrWarnBlock(doc, blockId);
        if (block == null) return;
        CharNode node = new CharNode(charId, parentId, ch);
        block.getContent().addChar(node);
    }

    private void applyDeleteChar(BlockCRDT doc, JSONObject json) {
        String blockId = json.getString("blockId");
        CharID targetId = parseCharID(json.getJSONObject("charId"));

        Block block = findOrWarnBlock(doc, blockId);
        if (block == null) return;
        new DeleteCharOperation(blockId, targetId).apply(block.getContent());
    }

    private void applyReplaceChar(BlockCRDT doc, JSONObject json) {
        String blockId = json.getString("blockId");
        CharID oldId = parseCharID(json.getJSONObject("oldCharId"));
        JSONObject newNodeJson = json.getJSONObject("newNode");
        CharID newId = parseCharID(newNodeJson.getJSONObject("charId"));
        CharID newParent = newNodeJson.isNull("parentId") ? null : parseCharID(newNodeJson.getJSONObject("parentId"));
        char ch = newNodeJson.getString("char").charAt(0);

        Block block = findOrWarnBlock(doc, blockId);
        if (block == null) return;
        CharNode newNode = new CharNode(newId, newParent, ch);
        new ReplaceCharOperation(blockId, oldId, newNode).apply(block.getContent());
    }

    private void applyFormatting(BlockCRDT doc, JSONObject json) {
        String blockId = json.getString("blockId");
        CharID targetId = parseCharID(json.getJSONObject("charId"));
        String formatType = json.getString("formatType");
        boolean value = json.getBoolean("value");

        Block block = findOrWarnBlock(doc, blockId);
        if (block == null) return;
        new FormattingOperation(targetId, formatType, value).apply(block.getContent());
    }

    private void applyInsertBlock(BlockCRDT doc, JSONObject json) {
        BlockID blockId = parseBlockID(json.getJSONObject("blockId"));
        BlockID parentId = json.isNull("parentBlockId") ? null : parseBlockID(json.getJSONObject("parentBlockId"));
        Block newBlock = new Block(blockId, parentId);
        doc.addBlock(newBlock);
    }

    private void applyDeleteBlock(BlockCRDT doc, JSONObject json) {
        BlockID targetId = parseBlockID(json.getJSONObject("blockId"));
        doc.deleteBlock(targetId);
    }

    private void applySplitBlock(BlockCRDT doc, JSONObject json) {
        BlockID targetId = parseBlockID(json.getJSONObject("targetBlockId"));
        int splitIndex = json.getInt("splitIndex");
        BlockID newBlockId = parseBlockID(json.getJSONObject("newBlockId"));
        doc.splitBlock(targetId, splitIndex, newBlockId);
    }

    private void applyMergeBlocks(BlockCRDT doc, JSONObject json) {
        BlockID firstId = parseBlockID(json.getJSONObject("firstBlockId"));
        BlockID secondId = parseBlockID(json.getJSONObject("secondBlockId"));
        doc.mergeBlocks(firstId, secondId);
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
        if (block == null) System.err.println("Block not found: " + blockIdStr);
        return block;
    }

    private String extractDocId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    // Auto-save all active documents every 30 seconds.
    // fixedDelay means: wait 30 s after the previous run finishes before running again.
    @Scheduled(fixedDelay = 30000)
    public void autoSaveAll() {
        Map<String, BlockCRDT> sessions = DocumentSession.getAllSessions();
        if (sessions.isEmpty()) return;

        System.out.println("[AutoSave] Saving " + sessions.size() + " active document(s)...");
        for (Map.Entry<String, BlockCRDT> entry : sessions.entrySet()) {
            String docId = entry.getKey();
            BlockCRDT doc = entry.getValue();
            try {
                synchronized (doc) {
                    persistenceService.saveDocument(doc, docId, docId);
                }
            } catch (Exception e) {
                System.err.println("[AutoSave] Failed to save '" + docId + "': " + e.getMessage());
            }
        }
    }
}