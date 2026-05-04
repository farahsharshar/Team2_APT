package com.Team2_CDE_master.ProjectServer.crdt;

public class DeleteCharOperation {
    String blockId;
    CharID targetId;

    public DeleteCharOperation(String blockId, CharID targetId) {
        this.blockId = blockId;
        this.targetId = targetId;
    }
    public void apply(CharCRDT crdt) {
        CharNode node = crdt.findNode(targetId);
        if (node != null) {
            node.markDeleted();
        }
    }
    public String getBlockId() { return blockId; }
    public CharID getTargetId() { return targetId; }
    public String toString() {
        return "DeleteOp[block=" + blockId + ", target=" + targetId + "]";
    }
}
