package com.Team2_CDE_master.ProjectServer.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Person C — Phase 3: Share Code Manager (client side)
 *
 * Responsible for:
 *   1. Fetching the editor + viewer codes from GET /api/share/{docId}
 *   2. Showing them to the user in a clean dialog with copy buttons
 *
 * This class must only be called when the local role is EDITOR.
 * ViewerMode blocks the share button in EditorWindow so viewers never reach here.
 */
public class ShareManager {

    private static final String SERVER_BASE = "http://localhost:8080";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * Fetches codes for docId and shows the share dialog.
     * Must be called on the EDT or will dispatch via SwingUtilities.
     */
    public static void showShareDialog(Component parent, String docId) {
        if (docId == null || docId.isBlank()) {
            JOptionPane.showMessageDialog(parent,
                    "No document is open.", "Share", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Fetch codes in a background thread so we don't block the EDT
        new Thread(() -> {
            try {
                String url = SERVER_BASE + "/api/share/"
                        + java.net.URLEncoder.encode(docId, StandardCharsets.UTF_8);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    String body = resp.body();
                    String editorCode = extractJson(body, "editorCode");
                    String viewerCode = extractJson(body, "viewerCode");
                    SwingUtilities.invokeLater(() ->
                            presentCodes(parent, docId, editorCode, viewerCode));
                } else if (resp.statusCode() == 404) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(parent,
                                    "Document not found on server.\n"
                                    + "Make sure the document is connected before sharing.",
                                    "Share Failed", JOptionPane.ERROR_MESSAGE));
                } else {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(parent,
                                    "Server returned " + resp.statusCode(),
                                    "Share Failed", JOptionPane.ERROR_MESSAGE));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parent,
                                "Cannot reach server:\n" + ex.getMessage(),
                                "Share Failed", JOptionPane.ERROR_MESSAGE));
            }
        }, "share-fetch").start();
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private static void presentCodes(Component parent, String docId,
                                     String editorCode, String viewerCode) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Title row
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        JLabel title = new JLabel("Share codes for: " + docId);
        title.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(title, gbc);

        gbc.gridwidth = 1;

        // Editor code row
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("✏️  Editor code:"), gbc);

        gbc.gridx = 1;
        JTextField editorField = makeCodeField(editorCode);
        panel.add(editorField, gbc);

        gbc.gridx = 2;
        panel.add(makeCopyButton(editorCode), gbc);

        // Viewer code row
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("👁  Viewer code:"), gbc);

        gbc.gridx = 1;
        JTextField viewerField = makeCodeField(viewerCode);
        panel.add(viewerField, gbc);

        gbc.gridx = 2;
        panel.add(makeCopyButton(viewerCode), gbc);

        // Footer hint
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        JLabel hint = new JLabel("<html><i>Share these codes over WhatsApp, email, etc.<br>"
                + "Viewers cannot edit and cannot see codes.</i></html>");
        hint.setFont(new Font("Arial", Font.PLAIN, 11));
        hint.setForeground(Color.GRAY);
        panel.add(hint, gbc);

        JOptionPane.showMessageDialog(parent, panel,
                "Share Document", JOptionPane.PLAIN_MESSAGE);
    }

    private static JTextField makeCodeField(String code) {
        JTextField f = new JTextField(code, 10);
        f.setFont(new Font("Courier New", Font.BOLD, 16));
        f.setEditable(false);
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setBackground(new Color(245, 245, 245));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        return f;
    }

    private static JButton makeCopyButton(String code) {
        JButton btn = new JButton("Copy");
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                   .getSystemClipboard()
                   .setContents(new StringSelection(code), null);
            btn.setText("Copied!");
            // Reset label after 1.5 s
            new Timer(1500, t -> { btn.setText("Copy"); ((Timer)t.getSource()).stop(); })
                    .start();
        });
        return btn;
    }

    /** Minimal JSON string field extractor. */
    private static String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf('"', start);
        return end < 0 ? "" : json.substring(start, end);
    }
}
