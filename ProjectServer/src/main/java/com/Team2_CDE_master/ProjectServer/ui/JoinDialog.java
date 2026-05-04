package com.Team2_CDE_master.ProjectServer.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class JoinDialog {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static JoinResult show(Component parent, String serverBase) {

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JLabel hint = new JLabel("<html>Paste the share code you received<br>(8 characters, letters and numbers):</html>");
        hint.setFont(new Font("Arial", Font.PLAIN, 13));

        JTextField codeField = new JTextField(14);
        codeField.setFont(new Font("Courier New", Font.BOLD, 18));
        codeField.setHorizontalAlignment(JTextField.CENTER);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        panel.add(hint,       BorderLayout.NORTH);
        panel.add(codeField,  BorderLayout.CENTER);
        panel.add(errorLabel, BorderLayout.SOUTH);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(
                    parent, panel,
                    "Join Document by Code",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (choice != JOptionPane.OK_OPTION) return null;

            String code = codeField.getText().trim().toUpperCase();
            if (code.length() != 8) {
                errorLabel.setText("Code must be exactly 8 characters.");
                continue;
            }

            try {
                String url = serverBase + "/api/share/join?code="
                        + java.net.URLEncoder.encode(code, StandardCharsets.UTF_8);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    String body = resp.body();
                    String docId = extractJson(body, "docId");
                    String role  = extractJson(body, "role");
                    return new JoinResult(docId, role);
                } else if (resp.statusCode() == 404) {
                    errorLabel.setText("Code not found. Check the code and try again.");
                } else {
                    errorLabel.setText("Server error (" + resp.statusCode() + "). Try again.");
                }
            } catch (Exception ex) {
                errorLabel.setText("Cannot reach server: " + ex.getMessage());
            }
        }
    }

    private static String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf('"', start);
        return end < 0 ? "" : json.substring(start, end);
    }

    public record JoinResult(String docId, String role) {
        public boolean isViewer() { return "VIEWER".equalsIgnoreCase(role); }
        public boolean isEditor() { return "EDITOR".equalsIgnoreCase(role); }
    }
}
