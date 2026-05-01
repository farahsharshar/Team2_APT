package com.Team2_CDE_master.ProjectServer.persistence;

import jakarta.persistence.*;

// Represents one Block from the BlockCRDT stored in the database.
// A block is identified by the (blockSiteId, blockCounter) pair — the BlockID.
@Entity
@Table(name = "blocks")
public class BlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which document this block belongs to
    private String documentId;

    // The block's unique CRDT identity: BlockID(siteId, counter)
    private int blockSiteId;
    private int blockCounter;

    // Parent block identity — null columns mean this is a root-level block
    private Integer parentSiteId;
    private Integer parentCounter;

    // Tombstone flag: true means this block was deleted but kept for CRDT ordering
    private boolean deleted;

    // The block's position in the allBlocks list at save time.
    // Used to restore blocks in the same order when loading.
    private int ordering;

    public BlockEntity() {}

    public Long getId() { return id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String v) { this.documentId = v; }

    public int getBlockSiteId() { return blockSiteId; }
    public void setBlockSiteId(int v) { this.blockSiteId = v; }

    public int getBlockCounter() { return blockCounter; }
    public void setBlockCounter(int v) { this.blockCounter = v; }

    public Integer getParentSiteId() { return parentSiteId; }
    public void setParentSiteId(Integer v) { this.parentSiteId = v; }

    public Integer getParentCounter() { return parentCounter; }
    public void setParentCounter(Integer v) { this.parentCounter = v; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean v) { this.deleted = v; }

    public int getOrdering() { return ordering; }
    public void setOrdering(int v) { this.ordering = v; }
}
