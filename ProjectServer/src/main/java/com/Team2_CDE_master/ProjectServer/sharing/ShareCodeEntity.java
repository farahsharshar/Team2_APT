package com.Team2_CDE_master.ProjectServer.sharing;

import jakarta.persistence.*;

@Entity
@Table(name = "share_codes")
public class ShareCodeEntity {

    @Id
    private String docId;

    @Column(nullable = false, length = 16, unique = true)
    private String editorCode;

    @Column(nullable = false, length = 16, unique = true)
    private String viewerCode;

    public ShareCodeEntity() {}

    public ShareCodeEntity(String docId, String editorCode, String viewerCode) {
        this.docId      = docId;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
    }

    public String getDocId()      { return docId; }
    public void setDocId(String d){ this.docId = d; }

    public String getEditorCode()        { return editorCode; }
    public void setEditorCode(String c)  { this.editorCode = c; }

    public String getViewerCode()        { return viewerCode; }
    public void setViewerCode(String c)  { this.viewerCode = c; }
}
