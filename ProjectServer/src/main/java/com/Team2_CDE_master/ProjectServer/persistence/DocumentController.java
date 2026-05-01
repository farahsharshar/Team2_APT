package com.Team2_CDE_master.ProjectServer.persistence;

import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;
import com.Team2_CDE_master.ProjectServer.session.DocumentSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// REST API for document persistence.
// All endpoints are under /api/documents.
//
// Typical workflow:
//   1. Clients connect via WebSocket → edits happen in memory
//   2. Auto-save (every 30 s) or POST /api/documents?docId=X saves to DB
//   3. On next server start, GET /api/documents/{id} reloads from DB
//   4. Clients reconnect via WebSocket and get the restored state
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentPersistenceService persistenceService;

    // POST /api/documents?docId=myDoc&name=My+Document
    // Saves the current in-memory session for docId to the database.
    // Returns the saved document's metadata.
    @PostMapping
    public ResponseEntity<?> saveDocument(
            @RequestParam String docId,
            @RequestParam(defaultValue = "") String name) {

        BlockCRDT doc = DocumentSession.get(docId);
        if (doc == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "No active session found for docId: " + docId
                            + ". Connect a client to this document first."));
        }

        String docName = name.isEmpty() ? docId : name;
        DocumentEntity saved;
        synchronized (doc) {
            saved = persistenceService.saveDocument(doc, docId, docName);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("updatedAt", saved.getUpdatedAt().toString());
        response.put("status", "saved");
        return ResponseEntity.ok(response);
    }

    // GET /api/documents
    // Lists all documents that have been saved to the database (metadata only).
    @GetMapping
    public List<DocumentEntity> listDocuments() {
        return persistenceService.listDocuments();
    }

    // GET /api/documents/{id}
    // Loads a document from the database and seeds the in-memory session.
    // After this call, WebSocket clients connecting to docId will see the saved state.
    // Returns the document's visible text and block count as a quick preview.
    @GetMapping("/{id}")
    public ResponseEntity<?> loadDocument(@PathVariable String id) {
        BlockCRDT doc = persistenceService.loadDocument(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        // Seed the in-memory session so the next WebSocket connection gets this state
        DocumentSession.seed(id, doc);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("blockCount", doc.getBlockCount());
        response.put("text", doc.getFullText());
        response.put("status", "loaded");
        return ResponseEntity.ok(response);
    }

    // DELETE /api/documents/{id}
    // Removes a document from the database permanently.
    // Does NOT affect any active in-memory session.
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable String id) {
        if (!persistenceService.documentExists(id)) {
            return ResponseEntity.notFound().build();
        }
        persistenceService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Deleted document: " + id));
    }
}
