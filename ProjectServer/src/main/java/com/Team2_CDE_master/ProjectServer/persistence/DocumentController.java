package com.Team2_CDE_master.ProjectServer.persistence;

import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;
import com.Team2_CDE_master.ProjectServer.session.DocumentSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentPersistenceService persistenceService;

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

    @GetMapping
    public List<DocumentEntity> listDocuments() {
        return persistenceService.listDocuments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> loadDocument(@PathVariable String id) {
        BlockCRDT doc = persistenceService.loadDocument(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        DocumentSession.seed(id, doc);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("blockCount", doc.getBlockCount());
        response.put("text", doc.getFullText());
        response.put("status", "loaded");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable String id) {
        if (!persistenceService.documentExists(id)) {
            return ResponseEntity.notFound().build();
        }
        persistenceService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Deleted document: " + id));
    }
}
