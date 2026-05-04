package com.Team2_CDE_master.ProjectServer.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "char_nodes")
public class CharNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String documentId;

    private int blockSiteId;
    private int blockCounter;

    private int charSiteId;
    private int charNum;

    private Integer parentSiteId;
    private Integer parentNum;

    @Column(length = 4)
    private String charValue;

    private boolean deleted;

    private boolean bold;
    private boolean italic;

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
