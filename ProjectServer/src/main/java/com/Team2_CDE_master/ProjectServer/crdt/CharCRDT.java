package com.Team2_CDE_master.ProjectServer.crdt;

import java.util.ArrayList;
import java.util.HashMap;

public class CharCRDT {
    public ArrayList<CharNode> allNodes;

    private final HashMap<String, CharNode> nodeMap = new HashMap<>();

    public CharCRDT() {
        allNodes = new ArrayList<>();
    }

    private String key(CharID id) {
        return id.siteId + "," + id.myNum;
    }

    public void addChar(CharNode newNode) {

        // ELHEBEISHY'S PART
        CharNode existing = nodeMap.get(key(newNode.getMyId()));
        if (existing != null) {
            if (existing.checkDeleted()) {
                existing.restore();
                existing.setBold(newNode.checkBold());
                existing.setItalic(newNode.checkItalic());
            }
            return;
        }

        int parentPos = -1;

        if (newNode.getParentId() != null) {
            for (int i = 0; i < allNodes.size(); i++) {
                if (allNodes.get(i).getMyId().isSameAs(newNode.getParentId())) {
                    parentPos = i;
                    break;
                }
            }
        }
        int insertAt = parentPos + 1;

        while (insertAt < allNodes.size()) {
            CharNode current = allNodes.get(insertAt);

            boolean sameParent = false;
            if (newNode.getParentId() == null && current.getParentId() == null) {
                sameParent = true;
            } else if (newNode.getParentId() != null && current.getParentId() != null) {
                sameParent = current.getParentId().isSameAs(newNode.getParentId());
            }

            if (sameParent) {
                if (current.getMyId().siteId > newNode.getMyId().siteId) {
                    insertAt++;
                } else if (current.getMyId().siteId == newNode.getMyId().siteId) {
                    if (current.getMyId().myNum > newNode.getMyId().myNum) {
                        insertAt++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        allNodes.add(insertAt, newNode);
        nodeMap.put(key(newNode.getMyId()), newNode);
    }

    public String getText() {
        StringBuilder result = new StringBuilder();
        for (CharNode node : allNodes) {
            if (!node.checkDeleted()) {
                result.append(node.getMyChar());
            }
        }
        return result.toString();
    }

    public CharNode findNode(CharID targetId) {
        return nodeMap.get(key(targetId));
    }

    public void applyFormatting(CharID targetId, String type, boolean value) {
        CharNode node = findNode(targetId);

        if (node == null) {
            return;
        }
        if (node.checkDeleted()) {
            return;
        }

        if (type.equals("bold")) {
            node.setBold(value);
        } else if (type.equals("italic")) {
            node.setItalic(value);
        }
    }

    public void printAll() {
        System.out.println("=== All Nodes (including deleted) ===");
        for (CharNode node : allNodes) {
            System.out.println("  " + node.toString());
        }
        System.out.println("Visible text: \"" + getText() + "\"");
    }

    public int getLength() {
        int count = 0;
        for (CharNode node : allNodes) {
            if (!node.checkDeleted()) {
                count++;
            }
        }
        return count;
    }
}
