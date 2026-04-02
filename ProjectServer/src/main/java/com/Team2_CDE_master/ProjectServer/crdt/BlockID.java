package com.Team2_CDE_master.ProjectServer.crdt;
public class BlockID {
    public int siteId; 
    public int counter; 

    public BlockID(int siteId, int counter) {
        this.siteId = siteId;
        this.counter = counter;
    }
    public boolean isSameAs(BlockID other) {
        if (other == null) return false;
        return this.siteId == other.siteId && this.counter == other.counter;
    }
    public String toString() {
        return "B" + siteId + "_" + counter;
    }
}
