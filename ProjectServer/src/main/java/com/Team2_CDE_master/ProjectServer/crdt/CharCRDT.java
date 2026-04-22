package com.Team2_CDE_master.ProjectServer.crdt;

import java.util.ArrayList;
import java.util.HashMap;

public class CharCRDT {
    // all the nodes — including deleted ones (tombstones)
    public ArrayList<CharNode> allNodes;

    private final HashMap<String, CharNode> nodeMap = new HashMap<>();

    public CharCRDT() {
        allNodes = new ArrayList<>();
    }

    private String key(CharID id) {
        return id.siteId + "," + id.myNum;
    }

    // insert a new character node into the list
    // it goes right after its parent
    // if two chars have the same parent (concurrent insert), we use siteId to break the tie
    public void addChar(CharNode newNode) {

        // find the position of the parent
        int parentPos = -1;   // -1 means the parent is the beginning (before all chars)

        if (newNode.getParentId() != null) {
            for (int i = 0; i < allNodes.size(); i++) {
                if (allNodes.get(i).getMyId().isSameAs(newNode.getParentId())) {
                    parentPos = i;
                    break;
                }
            }
        }
        // figure out where exactly to insert
        // we start right after the parent and skip any siblings that have a higher siteId
        int insertAt = parentPos + 1;

        while (insertAt < allNodes.size()) {
            CharNode current = allNodes.get(insertAt);

            // check if this node also has the same parent (it's a sibling / concurrent insert)
            boolean sameParent = false;
            if (newNode.getParentId() == null && current.getParentId() == null) {
                sameParent = true;
            } else if (newNode.getParentId() != null && current.getParentId() != null) {
                sameParent = current.getParentId().isSameAs(newNode.getParentId());
            }

//            if (sameParent) {
//                // tie-breaker: higher siteId goes first (left side)
//                if (current.getMyId().siteId > newNode.getMyId().siteId) {
//                    insertAt++;   // skip this sibling, it wins
//                } else {
//                    break;        // our new node wins, insert here
//                }
//            } else {
//                break;   // no more siblings, stop here
//            }
            //deterministic ordering (Farah Elhebeishy)
            if (sameParent) {
                // higher siteId is supposed to be typed first
                if (current.getMyId().siteId > newNode.getMyId().siteId) {
                    insertAt++;   // current wins, skip it
                } else if (current.getMyId().siteId == newNode.getMyId().siteId) {
                    // same siteId -> break tie using myNum, higher myNum goes first
                    if (current.getMyId().myNum > newNode.getMyId().myNum) {
                        insertAt++;   // current wins, skip it
                    } else {
                        break;        // new node wins, insert here
                    }
                } else {
                    break;   // new node wins (higher siteId), insert here
                }
            } else {
                break;   // no more siblings, stop here
            }
        }

        allNodes.add(insertAt, newNode);
        nodeMap.put(key(newNode.getMyId()), newNode);
    }

    // get the visible text — skip deleted characters
    public String getText() {
        StringBuilder result = new StringBuilder();
        for (CharNode node : allNodes) {
            if (!node.checkDeleted()) {
                result.append(node.getMyChar());
            }
        }
        return result.toString();
    }

    // find a node by its id (returns null if not found)
    public CharNode findNode(CharID targetId) {
        return nodeMap.get(key(targetId));
    }

    // applying bold or italic formatting to a node by its id
    // type = "bold" or "italic"
    // value = true to turn on, false to turn off
    // does nothing if the node is deleted or not found
    public void applyFormatting(CharID targetId, String type, boolean value) {
        CharNode node = findNode(targetId);

        // if node not found or already deleted, do nothing
        if (node == null) {
            return;
        }
        if (node.checkDeleted()) {
            return;
        }

        // apply the right formatting
        if (type.equals("bold")) {
            node.setBold(value);
        } else if (type.equals("italic")) {
            node.setItalic(value);
        }
        // if type is something else, just ignore it
    }

    // print all nodes including tombstones (for debugging)
    public void printAll() {
        System.out.println("=== All Nodes (including deleted) ===");
        for (CharNode node : allNodes) {
            System.out.println("  " + node.toString());
        }
        System.out.println("Visible text: \"" + getText() + "\"");
    }

    // how many visible (not deleted) characters are there
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