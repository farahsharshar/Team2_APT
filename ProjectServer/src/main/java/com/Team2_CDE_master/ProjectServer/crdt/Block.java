package com.Team2_CDE_master.ProjectServer.crdt;

public class Block {
    BlockID myId;
    BlockID parentId;
    CharCRDT content;
    boolean isDeleted;

    public Block(BlockID myId, BlockID parentId) {
        this.myId = myId;
        this.parentId = parentId;
        this.content = new CharCRDT();
        this.isDeleted = false;
    }
    public BlockID getMyId() { return myId; }
    public BlockID getParentId() { return parentId; }
    public CharCRDT getContent() { return content; }
    public boolean checkDeleted() { return isDeleted; }
    public void markDeleted() { this.isDeleted = true; }
    public String getText() {
        return content.getText();
    }
    public String toString() {
        String result = "Block[id=" + myId;
        if (parentId != null) result += ", parent=" + parentId;
        if (isDeleted) result += ", DELETED";
        result += ", text=\"" + getText() + "\"]";
        return result;
    }
}
