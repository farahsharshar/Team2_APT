package com.Team2_CDE_master.ProjectServer.crdt;

import java.util.ArrayList;

// this is the main character-level CRDT
// it holds a list of all character nodes in order
// think of it like a linked list but stored in an arraylist
public class CharCRDT {

    // all the nodes — including deleted ones (tombstones)
    ArrayList<CharNode> allNodes;

    public CharCRDT() {
        allNodes = new ArrayList<>();
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

        // now figure out where exactly to insert
        // we start right after the parent and skip any siblings that have a higher siteId
        // (higher siteId wins → goes first — Member 4 will refine this logic)
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

            if (sameParent) {
                // tie-breaker: higher siteId goes first (left side)
                if (current.getMyId().siteId > newNode.getMyId().siteId) {
                    insertAt++;   // skip this sibling, it wins
                } else {
                    break;        // our new node wins, insert here
                }
            } else {
                break;   // no more siblings, stop here
            }
        }

        allNodes.add(insertAt, newNode);
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
        for (CharNode node : allNodes) {
            if (node.getMyId().isSameAs(targetId)) {
                return node;
            }
        }
        return null;
    }

    // print all nodes including tombstones (for debugging)
    public void printAll() {
        System.out.println("=== All Nodes (including deleted) ===");
        for (CharNode node : allNodes) {
            System.out.println("  " + node.toString());
        }
        System.out.println("Visible text: \"" + getText() + "\"");
        System.out.println("=====================================");
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