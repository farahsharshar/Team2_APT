package com.Team2_CDE_master.ProjectServer.crdt;

public class CharID {
    public int siteId;
    public int myNum;

    public CharID(int siteId, int myNum) {
        this.siteId = siteId;
        this.myNum = myNum;
    }
    public boolean isSameAs(CharID other) {
        if (other == null) return false;
        return this.siteId == other.siteId && this.myNum == other.myNum;
    }
    public String toString() {
        return "(" + siteId + "," + myNum + ")";
    }
}
