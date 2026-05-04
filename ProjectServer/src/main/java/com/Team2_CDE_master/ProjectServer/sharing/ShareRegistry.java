package com.Team2_CDE_master.ProjectServer.sharing;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Person C — Phase 3: Sharing Codes + Permissions
 *
 * Holds two share codes per document:
 *   - editor code  → full read/write access
 *   - viewer code  → read-only access (cannot edit, cannot see codes)
 *
 * Codes are generated once per document and kept in memory.
 * They are also persisted to the database via ShareCodeEntity so they
 * survive server restarts (see ShareCodeRepository).
 */
@Component
public class ShareRegistry {

    // docId → pair of codes
    private final Map<String, ShareCodes> registry = new ConcurrentHashMap<>();

    private static final SecureRandom RNG = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final int CODE_LEN = 8;

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    /**
     * Returns the share codes for a document.
     * Creates and stores new codes if none exist yet.
     */
    public ShareCodes getOrCreate(String docId) {
        return registry.computeIfAbsent(docId, id ->
                new ShareCodes(generate(), generate()));
    }

    /**
     * Look up which document a code belongs to, and what role it grants.
     * Returns null if the code is unknown.
     */
    public CodeLookup lookup(String code) {
        for (Map.Entry<String, ShareCodes> e : registry.entrySet()) {
            ShareCodes sc = e.getValue();
            if (sc.editorCode().equals(code))  return new CodeLookup(e.getKey(), Role.EDITOR);
            if (sc.viewerCode().equals(code))  return new CodeLookup(e.getKey(), Role.VIEWER);
        }
        return null;
    }

    /**
     * Seed codes from the database on startup so they don't change after restart.
     */
    public void seed(String docId, String editorCode, String viewerCode) {
        registry.put(docId, new ShareCodes(editorCode, viewerCode));
    }

    // ------------------------------------------------------------------ //
    //  Internals
    // ------------------------------------------------------------------ //

    private static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  Value types
    // ------------------------------------------------------------------ //

    public record ShareCodes(String editorCode, String viewerCode) {}

    public record CodeLookup(String docId, Role role) {}

    public enum Role { EDITOR, VIEWER }
}
