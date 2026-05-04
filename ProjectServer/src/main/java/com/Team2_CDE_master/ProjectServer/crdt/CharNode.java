package com.Team2_CDE_master.ProjectServer.crdt;

public class CharNode {
    CharID myId;
    CharID parentId;
    char myChar;
    boolean isDeleted;
    boolean isBold;
    boolean isItalic;

    public CharNode(CharID myId, CharID parentId, char myChar) {
        this.myId = myId;
        this.parentId = parentId;
        this.myChar = myChar;
        this.isDeleted = false;
        this.isBold = false;
        this.isItalic = false;
    }
    public CharID getMyId() { return myId; }
    public CharID getParentId() { return parentId; }
    public char getMyChar() { return myChar; }
    public boolean checkDeleted() { return isDeleted; }
    public boolean checkBold() { return isBold; }
    public boolean checkItalic() { return isItalic; }
    public void markDeleted() { this.isDeleted = true; }
    // ELHEBEISHY'S PART
    public void restore() { this.isDeleted = false; }
    public void setBold(boolean val) { this.isBold = val; }
    public void setItalic(boolean val) { this.isItalic = val; }
    public String toString() {
        String result = "Node[id=" + myId + ", char='" + myChar + "'";
        if (isDeleted) result += ", DELETED";
        if (isBold) result += ", BOLD";
        if (isItalic) result += ", ITALIC";
        result += "]";
        return result;
    }
}
