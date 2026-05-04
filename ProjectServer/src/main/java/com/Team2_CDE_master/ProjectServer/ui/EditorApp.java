package com.Team2_CDE_master.ProjectServer.ui;

import javax.swing.*;


public class EditorApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EditorWindow window = new EditorWindow();
            window.setVisible(true);
        });
    }
}
