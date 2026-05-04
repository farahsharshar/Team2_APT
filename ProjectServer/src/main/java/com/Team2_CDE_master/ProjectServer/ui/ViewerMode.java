package com.Team2_CDE_master.ProjectServer.ui;

/**
 * Person C — Phase 3: Viewer Mode (client-side role enforcement)
 *
 * Stores whether the current client session is an EDITOR or VIEWER.
 * The role is set once when the user joins via a share code (JoinDialog)
 * or when they create / directly connect to a document (always EDITOR).
 *
 * The role is then checked in EditorWindow before every edit action:
 *   - key presses (insert / delete / enter)
 *   - bold / italic buttons
 *   - share button visibility
 *
 * Because the server also ignores operations from viewer sessions
 * (enforced in CRDTWebSocketHandler via the role header), a malicious
 * client that bypasses the UI still cannot write.
 */
public class ViewerMode {

    public enum Role { EDITOR, VIEWER }

    private static Role currentRole = Role.EDITOR;   // default: full access

    /** Set the role for this session. Call once, right after joining. */
    public static void setRole(Role role) {
        currentRole = role;
    }

    /** Convenience setter from a string ("EDITOR" / "VIEWER"). */
    public static void setRole(String roleStr) {
        currentRole = "VIEWER".equalsIgnoreCase(roleStr) ? Role.VIEWER : Role.EDITOR;
    }

    /** True if the user may edit text, apply formatting, and see share codes. */
    public static boolean isEditor() {
        return currentRole == Role.EDITOR;
    }

    /** True if the user is read-only. */
    public static boolean isViewer() {
        return currentRole == Role.VIEWER;
    }

    /** Human-readable label for status bar. */
    public static String label() {
        return currentRole == Role.VIEWER ? "👁 Viewer" : "✏️ Editor";
    }

    /** Resets to EDITOR — call when a new document session begins from scratch. */
    public static void reset() {
        currentRole = Role.EDITOR;
    }
}
