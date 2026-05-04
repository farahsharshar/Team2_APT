package com.Team2_CDE_master.ProjectServer.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "blocks")
public class BlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String documentId;

    private int blockSiteId;
    private int blockCounter;

    private Integer parentSiteId;
    private Integer parentCounter;

    private boolean deleted;

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
