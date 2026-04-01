package com.Team2_CDE_master.ProjectServer.crdt;

// this represents one single block in the document
// a block is a logical unit that contains a sequence of characters (its own CharCRDT)
// think of a block like a paragraph — each paragraph has its own character list inside
public class Block {

    BlockID myId;           // unique id of this block
    BlockID parentId;       // id of the block directly above this one
                            // null if this is the very first block in the document

    CharCRDT content;       // all the characters inside this block

    boolean isDeleted;      // tombstone flag — true means this block was deleted
                            // we keep it in the list but skip it when rendering

    // constructor — creates a new empty block
    public Block(BlockID myId, BlockID parentId) {
        this.myId = myId;
        this.parentId = parentId;
        this.content = new CharCRDT();
        this.isDeleted = false;
    }

    // getters
    public BlockID getMyId() { return myId; }
    public BlockID getParentId() { return parentId; }
    public CharCRDT getContent() { return content; }
    public boolean checkDeleted() { return isDeleted; }

    // tombstone this block
    public void markDeleted() { this.isDeleted = true; }

    // get the visible text inside this block
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
