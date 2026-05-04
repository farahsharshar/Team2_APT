package com.Team2_CDE_master.ProjectServer.sharing;

import com.Team2_CDE_master.ProjectServer.session.DocumentSession;
import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private ShareRegistry shareRegistry;

    @Autowired
    private ShareCodeRepository shareCodeRepo;

    @GetMapping("/{docId}")
    public ResponseEntity<?> getCodes(@PathVariable String docId) {
        // Ensure session exists — getOrCreate is safe (returns existing if present).
        // Previously used DocumentSession.get() which returned null and caused 404
        // whenever the session hadn't been seeded yet.
        DocumentSession.getOrCreate(docId);

        ShareRegistry.ShareCodes codes = shareRegistry.getOrCreate(docId);

        persistIfNew(docId, codes);

        return ResponseEntity.ok(Map.of(
                "docId",      docId,
                "editorCode", codes.editorCode(),
                "viewerCode", codes.viewerCode()
        ));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinByCode(@RequestParam String code) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }

        ShareRegistry.CodeLookup result = shareRegistry.lookup(code.trim().toUpperCase());
        if (result == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Invalid or expired code"));
        }

        return ResponseEntity.ok(Map.of(
                "docId", result.docId(),
                "role",  result.role().name()
        ));
    }

    private void persistIfNew(String docId, ShareRegistry.ShareCodes codes) {
        Optional<ShareCodeEntity> existing = shareCodeRepo.findById(docId);
        if (existing.isEmpty()) {
            shareCodeRepo.save(new ShareCodeEntity(docId, codes.editorCode(), codes.viewerCode()));
        }
    }
}