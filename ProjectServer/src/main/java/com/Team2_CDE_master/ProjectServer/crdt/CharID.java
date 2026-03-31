package com.Team2_CDE_master.ProjectServer.crdt;
// this class just holds the id of one character
// every character has an id = (who made it, which number it is)
public class CharID {

    int siteId;    // which user inserted this char (user 1, user 2, etc.)
    int myNum;     // the counter — how many chars this user made before this one

    public CharID(int siteId, int myNum) {
        this.siteId = siteId;
        this.myNum = myNum;
    }

    // check if two ids are exactly the same
    public boolean isSameAs(CharID other) {
        if (other == null) return false;
        return this.siteId == other.siteId && this.myNum == other.myNum;
    }

    public String toString() {
        return "(" + siteId + "," + myNum + ")";
    }
}