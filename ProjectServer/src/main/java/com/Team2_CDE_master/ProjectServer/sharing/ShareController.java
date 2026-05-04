package com.Team2_CDE_master.ProjectServer.sharing;

import com.Team2_CDE_master.ProjectServer.session.DocumentSession;
import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Person C — Phase 3: Sharing REST endpoints
 *
 * GET  /api/share/{docId}           → returns editor + viewer codes (caller must be EDITOR)
 *                                      The UI enforces this — viewers never call this endpoint.
 *
 * POST /api/share/join?code=XXXX    → resolves a code to { docId, role }
 *                                      Client sends a code typed/pasted by the user and
 *                                      gets back which document to open and in what role.
 */
@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private ShareRegistry shareRegistry;

    @Autowired
    private ShareCodeRepository shareCodeRepo;

    // ------------------------------------------------------------------ //
    //  GET /api/share/{docId}
    //  Returns the two codes for a document.
    //  The UI must only call this for EDITOR-role clients.
    // ------------------------------------------------------------------ //
    @GetMapping("/{docId}")
    public ResponseEntity<?> getCodes(@PathVariable String docId) {
        // Make sure the document actually exists (has an active session or was saved)
        BlockCRDT session = DocumentSession.get(docId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        ShareRegistry.ShareCodes codes = shareRegistry.getOrCreate(docId);

        // Persist codes to DB so they survive restarts
        persistIfNew(docId, codes);

        return ResponseEntity.ok(Map.of(
                "docId",      docId,
                "editorCode", codes.editorCode(),
                "viewerCode", codes.viewerCode()
        ));
    }

    // ------------------------------------------------------------------ //
    //  POST /api/share/join?code=XXXXXXXX
    //  Resolves a share code → { docId, role } so the client knows
    //  which WebSocket room to join and whether it is an editor or viewer.
    // ------------------------------------------------------------------ //
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
                "role",  result.role().name()   // "EDITOR" or "VIEWER"
        ));
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers
    // ------------------------------------------------------------------ //

    private void persistIfNew(String docId, ShareRegistry.ShareCodes codes) {
        Optional<ShareCodeEntity> existing = shareCodeRepo.findById(docId);
        if (existing.isEmpty()) {
            shareCodeRepo.save(new ShareCodeEntity(docId, codes.editorCode(), codes.viewerCode()));
        }
    }
}
