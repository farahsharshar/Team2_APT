package com.Team2_CDE_master.ProjectServer.client;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import com.Team2_CDE_master.ProjectServer.network.OperationListener;
import org.json.JSONObject;
import java.util.function.BiConsumer;


public class OperationApplier implements OperationListener {

    private final BlockCRDT localDoc;

    private Runnable onDocumentChanged;

    private Runnable onConnected;

    private Runnable onDisconnected;

    private BiConsumer<Integer, Integer> onCursorUpdate;

    public OperationApplier(BlockCRDT localDoc) {
        this.localDoc = localDoc;
    }

    public void setOnDocumentChanged(Runnable r)              { this.onDocumentChanged = r; }
    public void setOnConnected(Runnable r)                    { this.onConnected = r; }
    public void setOnDisconnected(Runnable r)                 { this.onDisconnected = r; }
    public void setOnCursorUpdate(BiConsumer<Integer, Integer> c) { this.onCursorUpdate = c; }


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
        if (block != null) block.getContent().addChar(new CharNode(charId, parentId, ch));
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
        if (localDoc.findBlock(id) == null) {
            localDoc.addBlock(new Block(id, parent));
        }
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
