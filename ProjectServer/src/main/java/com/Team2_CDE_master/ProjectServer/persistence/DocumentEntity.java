package com.Team2_CDE_master.ProjectServer.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Represents a saved document in the database.
// Stores only metadata — the actual CRDT content is in BlockEntity and CharNodeEntity.
@Entity
@Table(name = "documents")
public class DocumentEntity {

    // The document's unique ID — same string used in the WebSocket URL (/document/{docId})
    @Id
    private String id;

    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentEntity() {}

    public DocumentEntity(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
