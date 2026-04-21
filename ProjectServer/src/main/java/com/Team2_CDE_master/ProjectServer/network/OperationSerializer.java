package com.Team2_CDE_master.ProjectServer.network;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import org.json.JSONObject;

// takes operation parameters and give back working jason ready

public class OperationSerializer {
    public static String insertChar(String blockId, CharID charId, CharID parentId, char ch) {
        JSONObject json = new JSONObject();
        json.put("type", "insert_char");
        json.put("blockId", blockId);
        json.put("charId", charIdToJson(charId));
        if (parentId != null) {
            json.put("parentId", charIdToJson(parentId));
        } else {
            json.put("parentId", JSONObject.NULL);
        }
        json.put("char", String.valueOf(ch));
        return json.toString();
    }

    public static String deleteChar(String blockId, CharID charId) {
        JSONObject json = new JSONObject();
        json.put("type", "delete_char");
        json.put("blockId", blockId);
        json.put("charId", charIdToJson(charId));
        return json.toString();
    }

    public static String replaceChar(String blockId, CharID oldCharId, CharNode newNode) {
        JSONObject json = new JSONObject();
        json.put("type", "replace_char");
        json.put("blockId", blockId);
        json.put("oldCharId", charIdToJson(oldCharId));

        JSONObject newNodeJson = new JSONObject();
        newNodeJson.put("charId", charIdToJson(newNode.getMyId()));
        if (newNode.getParentId() != null) {
            newNodeJson.put("parentId", charIdToJson(newNode.getParentId()));
        } else {
            newNodeJson.put("parentId", JSONObject.NULL);
        }
        newNodeJson.put("char", String.valueOf(newNode.getMyChar()));
        json.put("newNode", newNodeJson);
        return json.toString();
    }

    public static String formatting(String blockId, CharID charId, String formatType, boolean value) {
        JSONObject json = new JSONObject();
        json.put("type", "formatting");
        json.put("blockId", blockId);
        json.put("charId", charIdToJson(charId));
        json.put("formatType", formatType);
        json.put("value", value);
        return json.toString();
    }


    public static String insertBlock(BlockID blockId, BlockID parentBlockId) {
        JSONObject json = new JSONObject();
        json.put("type", "insert_block");
        json.put("blockId", blockIdToJson(blockId));
        if (parentBlockId != null) {
            json.put("parentBlockId", blockIdToJson(parentBlockId));
        } else {
            json.put("parentBlockId", JSONObject.NULL);
        }
        return json.toString();
    }

    public static String deleteBlock(BlockID blockId) {
        JSONObject json = new JSONObject();
        json.put("type", "delete_block");
        json.put("blockId", blockIdToJson(blockId));
        return json.toString();
    }


    public static String splitBlock(BlockID targetBlockId, int splitIndex, BlockID newBlockId) {
        JSONObject json = new JSONObject();
        json.put("type", "split_block");
        json.put("targetBlockId", blockIdToJson(targetBlockId));
        json.put("splitIndex", splitIndex);
        json.put("newBlockId", blockIdToJson(newBlockId));
        return json.toString();
    }

    public static String mergeBlocks(BlockID firstBlockId, BlockID secondBlockId) {
        JSONObject json = new JSONObject();
        json.put("type", "merge_blocks");
        json.put("firstBlockId", blockIdToJson(firstBlockId));
        json.put("secondBlockId", blockIdToJson(secondBlockId));
        return json.toString();
    }


    private static JSONObject charIdToJson(CharID id) {
        JSONObject json = new JSONObject();
        json.put("siteId", id.siteId);
        json.put("myNum", id.myNum);
        return json;
    }

    private static JSONObject blockIdToJson(BlockID id) {
        JSONObject json = new JSONObject();
        json.put("siteId", id.siteId);
        json.put("counter", id.counter);
        return json;
    }
}
