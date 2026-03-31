package com.Team2_CDE_master.ProjectServer.crdt;

// this class represents one insert operation
// it wraps the character node that needs to be inserted
// we'll use this in Phase 2 to send operations over the network
public class InsertOperation {

    String blockId;       // which block this insert is happening in
    CharNode charToAdd;   // the actual character node to insert

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