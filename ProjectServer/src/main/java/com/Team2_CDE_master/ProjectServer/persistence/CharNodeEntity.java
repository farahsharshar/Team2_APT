package com.Team2_CDE_master.ProjectServer.persistence;

import jakarta.persistence.*;

// Represents one CharNode from a CharCRDT stored in the database.
// Each row is a single character (including tombstoned/deleted ones).
@Entity
@Table(name = "char_nodes")
public class CharNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which document this char node belongs to
    private String documentId;

    // Which block this char node lives inside — matches a BlockEntity row
    private int blockSiteId;
    private int blockCounter;

    // This character's unique CRDT identity: CharID(siteId, myNum)
    private int charSiteId;
    private int charNum;

    // Parent char identity — null columns mean this char has no predecessor (first in block)
    private Integer parentSiteId;
    private Integer parentNum;

    // The actual character — stored as a single-character String to avoid type mapping issues
    @Column(length = 4)
    private String charValue;

    // Tombstone: true means deleted but kept for CRDT ordering
    private boolean deleted;

    private boolean bold;
    private boolean italic;

    // Position in the allNodes list at save time — used to restore in the same order
    private int ordering;

    public CharNodeEntity() {}

    public Long getId() { return id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String v) { this.documentId = v; }

    public int getBlockSiteId() { return blockSiteId; }
    public void setBlockSiteId(int v) { this.blockSiteId = v; }

    public int getBlockCounter() { return blockCounter; }
    public void setBlockCounter(int v) { this.blockCounter = v; }

    public int getCharSiteId() { return charSiteId; }
    public void setCharSiteId(int v) { this.charSiteId = v; }

    public int getCharNum() { return charNum; }
    public void setCharNum(int v) { this.charNum = v; }

    public Integer getParentSiteId() { return parentSiteId; }
    public void setParentSiteId(Integer v) { this.parentSiteId = v; }

    public Integer getParentNum() { return parentNum; }
    public void setParentNum(Integer v) { this.parentNum = v; }

    public String getCharValue() { return charValue; }
    public void setCharValue(String v) { this.charValue = v; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean v) { this.deleted = v; }

    public boolean isBold() { return bold; }
    public void setBold(boolean v) { this.bold = v; }

    public boolean isItalic() { return italic; }
    public void setItalic(boolean v) { this.italic = v; }

    public int getOrdering() { return ordering; }
    public void setOrdering(int v) { this.ordering = v; }
}
