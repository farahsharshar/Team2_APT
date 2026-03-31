package crdt;

// this represents one single character in the document
public class CharNode {

    CharID myId;
    CharID parentId;

    char myChar;

    boolean isDeleted;    // tombstone flag — true means this char was deleted
    // we keep it in the list but just don't show it

    boolean isBold;
    boolean isItalic;

    public CharNode(CharID myId, CharID parentId, char myChar) {
        this.myId = myId;
        this.parentId = parentId;
        this.myChar = myChar;
        this.isDeleted = false;   // not deleted when first created
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