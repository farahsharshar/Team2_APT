package com.Team2_CDE_master.ProjectServer.crdt;

public class ReplaceCharOperation {

    String blockId;
    CharID targetId;
    CharNode newNode;

    public ReplaceCharOperation(String blockId, CharID targetId, CharNode newNode) {
        this.blockId = blockId;
        this.targetId = targetId;
        this.newNode = newNode;
    }

    public void apply(CharCRDT crdt) {
        // step 1 — tombstone the old character
        CharNode oldNode = crdt.findNode(targetId);
        if (oldNode != null) {
            oldNode.markDeleted();
        }

        // step 2 — insert the new character in its place
        crdt.addChar(newNode);
    }

    public String getBlockId() { return blockId; }
    public CharID getTargetId() { return targetId; }
    public CharNode getNewNode() { return newNode; }

    public String toString() {
        return "ReplaceOp[block=" + blockId + ", replaces=" + targetId + ", with=" + newNode + "]";
    }
}
