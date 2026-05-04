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
        BlockCRDT session = DocumentSession.get(docId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

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
