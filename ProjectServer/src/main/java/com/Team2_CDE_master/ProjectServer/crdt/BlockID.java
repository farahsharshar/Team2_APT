package com.Team2_CDE_master.ProjectServer.crdt;

// this class holds the unique id of one block
// every block has an id = "B" + siteId + "_" + counter
// example: "B1_1" means user 1 created this block and it was their 1st block
public class BlockID {

    public int siteId;     // which user created this block
    public int counter;    // how many blocks this user created before this one

    public BlockID(int siteId, int counter) {
        this.siteId = siteId;
        this.counter = counter;
    }

    // check if two block ids are exactly the same
    public boolean isSameAs(BlockID other) {
        if (other == null) return false;
        return this.siteId == other.siteId && this.counter == other.counter;
    }

    // returns the agreed-upon string format: "B<siteId>_<counter>"
    public String toString() {
        return "B" + siteId + "_" + counter;
    }
}