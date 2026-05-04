package com.Team2_CDE_master.ProjectServer.crdt;

public class InsertOperation {
    String blockId;
    CharNode charToAdd;

    public InsertOperation(String blockId, CharNode charToAdd) {
        this.blockId = blockId;
        this.charToAdd = charToAdd;
    }
    public String getBlockId() { return blockId; }
    public CharNode getCharToAdd() { return charToAdd; }
    public String toString() {
        return "InsertOp[block=" + blockId + ", node=" + charToAdd + "]";
    }
}
