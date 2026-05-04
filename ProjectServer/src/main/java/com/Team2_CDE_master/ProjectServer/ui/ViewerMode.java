package com.Team2_CDE_master.ProjectServer.ui;

public class ViewerMode {

    public enum Role { EDITOR, VIEWER }

    private static Role currentRole = Role.EDITOR;

    public static void setRole(Role role) {
        currentRole = role;
    }

    public static void setRole(String roleStr) {
        currentRole = "VIEWER".equalsIgnoreCase(roleStr) ? Role.VIEWER : Role.EDITOR;
    }

    public static boolean isEditor() {
        return currentRole == Role.EDITOR;
    }

    public static boolean isViewer() {
        return currentRole == Role.VIEWER;
    }

    public static String label() {
        return currentRole == Role.VIEWER ? "👁 Viewer" : "✏️ Editor";
    }

    public static void reset() {
        currentRole = Role.EDITOR;
    }
}
