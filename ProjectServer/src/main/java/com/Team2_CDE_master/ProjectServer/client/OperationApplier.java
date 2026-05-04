package com.Team2_CDE_master.ProjectServer.client;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import com.Team2_CDE_master.ProjectServer.network.OperationListener;
import org.json.JSONObject;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class OperationApplier implements OperationListener {

    private final BlockCRDT localDoc;

    private Runnable onDocumentChanged;

    private Runnable onConnected;

    private Runnable onDisconnected;

    private BiConsumer<Integer, Integer> onCursorUpdate;

    private BiConsumer<Integer, String> onPresenceJoin;

    private Consumer<Integer> onPresenceLeave;

    // ELHEBEISHY'S PART
    private Runnable onFullSyncApplied;

    // ELHEBEISHY'S PART
    private Consumer<String> onErrorMessage;

    public OperationApplier(BlockCRDT localDoc) {
        this.localDoc = localDoc;
    }

    public void setOnDocumentChanged(Runnable r)              { this.onDocumentChanged = r; }
    public void setOnConnected(Runnable r)                    { this.onConnected = r; }
    public void setOnDisconnected(Runnable r)                 { this.onDisconnected = r; }
    public void setOnCursorUpdate(BiConsumer<Integer, Integer> c) { this.onCursorUpdate = c; }
    public void setOnPresenceJoin(BiConsumer<Integer, String> c) { this.onPresenceJoin = c; }
    public void setOnPresenceLeave(Consumer<Integer> c) { this.onPresenceLeave = c; }
    // ELHEBEISHY'S PART
    public void setOnFullSyncApplied(Runnable r) { this.onFullSyncApplied = r; }
    // ELHEBEISHY'S PART
    public void setOnErrorMessage(Consumer<String> c) { this.onErrorMessage = c; }

    @Override
    public void onConnected() {
        if (onConnected != null) onConnected.run();
    }

    @Override
    public void onDisconnected() {
        if (onDisconnected != null) onDisconnected.run();
    }

    @Override
    public void onError(String errorMessage) {
        System.err.println("[OperationApplier] Error: " + errorMessage);
        // ELHEBEISHY'S PART
        if (onErrorMessage != null) onErrorMessage.accept(errorMessage);
    }

    @Override
    public void onOperationReceived(String jsonPayload) {
        try {
            JSONObject json = new JSONObject(jsonPayload);
            String type = json.getString("type");

            if (type.equals("cursor_update")) {
                if (onCursorUpdate != null) {
                    onCursorUpdate.accept(json.getInt("siteId"), json.getInt("position"));
                }
                return;
            }

            if (type.equals("presence")) {
                String action = json.getString("action");
                int sid = json.getInt("siteId");
                if (action.equals("join") && onPresenceJoin != null) {
                    String uname = json.optString("username", "User" + sid);
                    onPresenceJoin.accept(sid, uname);
                } else if (action.equals("leave") && onPresenceLeave != null) {
                    onPresenceLeave.accept(sid);
                }
                return;
            }
            if (type.equals("full_sync")) {
                applyFullSync(json);
                if (onDocumentChanged != null) onDocumentChanged.run();
                // ELHEBEISHY'S PART
                if (onFullSyncApplied != null) onFullSyncApplied.run();
                return;
            }

            synchronized (localDoc) {
                applyOp(type, json);
            }

            if (onDocumentChanged != null) onDocumentChanged.run();

        } catch (Exception e) {
            System.err.println("[OperationApplier] Failed to apply op: " + e.getMessage());
        }
    }


    private void applyOp(String type, JSONObject json) {
        switch (type) {
            case "insert_char"  -> applyInsertChar(json);
            case "delete_char"  -> applyDeleteChar(json);
            case "replace_char" -> applyReplaceChar(json);
            case "formatting"   -> applyFormatting(json);
            case "insert_block" -> applyInsertBlock(json);
            case "delete_block" -> applyDeleteBlock(json);
            case "split_block"  -> applySplitBlock(json);
            case "merge_blocks" -> applyMergeBlocks(json);
            default -> System.err.println("[OperationApplier] Unknown type: " + type);
        }
    }

    private void applyInsertChar(JSONObject json) {
        String blockIdStr = json.getString("blockId");
        CharID charId   = parseCharID(json.getJSONObject("charId"));
        CharID parentId = json.isNull("parentId") ? null : parseCharID(json.getJSONObject("parentId"));
        char ch = json.getString("char").charAt(0);
        Block block = findBlock(blockIdStr);
        // ELHEBEISHY'S PART
        if (block != null) {
            CharNode node = new CharNode(charId, parentId, ch);
            node.setBold(json.optBoolean("bold", false));
            node.setItalic(json.optBoolean("italic", false));
            block.getContent().addChar(node);
        }
    }

    private void applyDeleteChar(JSONObject json) {
        Block block = findBlock(json.getString("blockId"));
        if (block == null) return;
        CharNode node = block.getContent().findNode(parseCharID(json.getJSONObject("charId")));
        if (node != null) node.markDeleted();
    }

    private void applyReplaceChar(JSONObject json) {
        Block block = findBlock(json.getString("blockId"));
        if (block == null) return;
        CharNode old = block.getContent().findNode(parseCharID(json.getJSONObject("oldCharId")));
        if (old != null) old.markDeleted();
        JSONObject n = json.getJSONObject("newNode");
        CharID newId     = parseCharID(n.getJSONObject("charId"));
        CharID newParent = n.isNull("parentId") ? null : parseCharID(n.getJSONObject("parentId"));
        block.getContent().addChar(new CharNode(newId, newParent, n.getString("char").charAt(0)));
    }

    private void applyFormatting(JSONObject json) {
        Block block = findBlock(json.getString("blockId"));
        if (block == null) return;
        block.getContent().applyFormatting(
            parseCharID(json.getJSONObject("charId")),
            json.getString("formatType"),
            json.getBoolean("value")
        );
    }

    private void applyInsertBlock(JSONObject json) {
        BlockID id     = parseBlockID(json.getJSONObject("blockId"));
        BlockID parent = json.isNull("parentBlockId") ? null : parseBlockID(json.getJSONObject("parentBlockId"));
        // ELHEBEISHY'S PART
        localDoc.addBlock(new Block(id, parent));
    }

    private void applyDeleteBlock(JSONObject json) {
        localDoc.deleteBlock(parseBlockID(json.getJSONObject("blockId")));
    }

    private void applySplitBlock(JSONObject json) {
        localDoc.splitBlock(
            parseBlockID(json.getJSONObject("targetBlockId")),
            json.getInt("splitIndex"),
            parseBlockID(json.getJSONObject("newBlockId"))
        );
    }

    private void applyMergeBlocks(JSONObject json) {
        localDoc.mergeBlocks(
            parseBlockID(json.getJSONObject("firstBlockId")),
            parseBlockID(json.getJSONObject("secondBlockId"))
        );
    }
    private void applyFullSync(org.json.JSONObject json) {
        org.json.JSONArray blocks = json.getJSONArray("blocks");

        synchronized (localDoc) {
            // ELHEBEISHY'S PART
            localDoc.clear();

            for (int i = 0; i < blocks.length(); i++) {
                org.json.JSONObject blockJson = blocks.getJSONObject(i);

                BlockID blockId = new BlockID(
                        blockJson.getInt("siteId"),
                        blockJson.getInt("counter"));

                BlockID parentId = blockJson.isNull("parentId") ? null :
                        new BlockID(
                                blockJson.getJSONObject("parentId").getInt("siteId"),
                                blockJson.getJSONObject("parentId").getInt("counter"));

                // Only add the block if not already present
                if (localDoc.findBlock(blockId) == null) {
                    localDoc.addBlock(new Block(blockId, parentId));
                }

                Block block = localDoc.findBlock(blockId);
                if (block == null) continue;

                org.json.JSONArray chars = blockJson.getJSONArray("chars");
                for (int j = 0; j < chars.length(); j++) {
                    org.json.JSONObject charJson = chars.getJSONObject(j);

                    CharID charId = new CharID(
                            charJson.getInt("siteId"),
                            charJson.getInt("myNum"));

                    // Skip if already present
                    if (block.getContent().findNode(charId) != null) continue;

                    CharID charParent = charJson.isNull("parentId") ? null :
                            new CharID(
                                    charJson.getJSONObject("parentId").getInt("siteId"),
                                    charJson.getJSONObject("parentId").getInt("myNum"));

                    char ch = charJson.getString("char").charAt(0);
                    CharNode node = new CharNode(charId, charParent, ch);

                    if (charJson.getBoolean("deleted")) node.markDeleted();
                    node.setBold(charJson.getBoolean("bold"));
                    node.setItalic(charJson.getBoolean("italic"));

                    block.getContent().addChar(node);
                }
            }
        }
    }

    private CharID parseCharID(JSONObject j) {
        return new CharID(j.getInt("siteId"), j.getInt("myNum"));
    }

    private BlockID parseBlockID(JSONObject j) {
        return new BlockID(j.getInt("siteId"), j.getInt("counter"));
    }

    private Block findBlock(String blockIdStr) {
        String[] parts = blockIdStr.replace("B", "").split("_");
        BlockID id = new BlockID(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        Block block = localDoc.findBlock(id);
        if (block == null) System.err.println("[OperationApplier] Block not found: " + blockIdStr);
        return block;
    }
}
