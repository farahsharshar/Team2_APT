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

        org.json.JSONArray blocksJson = new org.json.JSONArray();
        for (com.Team2_CDE_master.ProjectServer.crdt.Block block : doc.allBlocks) {
            if (block.checkDeleted()) continue;

            org.json.JSONObject blockJson = new org.json.JSONObject();
            blockJson.put("siteId",  block.getMyId().siteId);
            blockJson.put("counter", block.getMyId().counter);

            if (block.getParentId() != null) {
                org.json.JSONObject p = new org.json.JSONObject();
                p.put("siteId",  block.getParentId().siteId);
                p.put("counter", block.getParentId().counter);
                blockJson.put("parentId", p);
            } else {
                blockJson.put("parentId", org.json.JSONObject.NULL);
            }

            org.json.JSONArray charsJson = new org.json.JSONArray();
            for (com.Team2_CDE_master.ProjectServer.crdt.CharNode node : block.getContent().allNodes) {
                org.json.JSONObject charJson = new org.json.JSONObject();
                charJson.put("siteId",  node.getMyId().siteId);
                charJson.put("myNum",   node.getMyId().myNum);

                if (node.getParentId() != null) {
                    org.json.JSONObject cp = new org.json.JSONObject();
                    cp.put("siteId", node.getParentId().siteId);
                    cp.put("myNum",  node.getParentId().myNum);
                    charJson.put("parentId", cp);
                } else {
                    charJson.put("parentId", org.json.JSONObject.NULL);
                }

                charJson.put("char",    String.valueOf(node.getMyChar()));
                charJson.put("deleted", node.checkDeleted());
                charJson.put("bold",    node.checkBold());
                charJson.put("italic",  node.checkItalic());
                charsJson.put(charJson);
            }
            blockJson.put("chars", charsJson);
            blocksJson.put(blockJson);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id",         id);
        response.put("blockCount", doc.getBlockCount());
        response.put("status",     "loaded");
        response.put("blocks",     blocksJson.toString());
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