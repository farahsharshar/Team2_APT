package com.Team2_CDE_master.ProjectServer.ui;

import com.Team2_CDE_master.ProjectServer.crdt.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Supplier;

public class FileManager {

    public interface FileActionListener {
        void onDocumentLoaded(String docId, BlockCRDT doc);
        void onStatusMessage(String message);
    }

    private final Component parentComponent;
    private final Supplier<String> serverBaseSupplier;
    private FileActionListener actionListener;
    private int siteId;

    private final HttpClient http = HttpClient.newHttpClient();

    public FileManager(Component parentComponent, Supplier<String> serverBaseSupplier) {
        this.parentComponent = parentComponent;
        this.serverBaseSupplier = serverBaseSupplier;
    }
    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public void setFileActionListener(FileActionListener listener) {
        this.actionListener = listener;
    }

    public void createNewDocument() {
        String docId = JOptionPane.showInputDialog(
                parentComponent,
                "Enter a name for the new document:",
                "New Document",
                JOptionPane.PLAIN_MESSAGE);

        if (docId == null || docId.trim().isEmpty()) {
            notify("New document cancelled.");
            return;
        }
        docId = docId.trim();

        BlockCRDT newDoc = new BlockCRDT();

        BlockID firstBlockId = new BlockID(siteId, 1);
        Block firstBlock = new Block(firstBlockId, null);
        newDoc.addBlock(firstBlock);

        notify("New document '" + docId + "' created.");
        if (actionListener != null) actionListener.onDocumentLoaded(docId, newDoc);
    }

    public void importFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import .txt File");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        int result = chooser.showOpenDialog(parentComponent);
        if (result != JFileChooser.APPROVE_OPTION) {
            notify("Import cancelled.");
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            java.util.List<String> lines =
                    Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

            if (lines.isEmpty()) {
                notify("The selected file is empty.");
                return;
            }

            BlockCRDT importedDoc = new BlockCRDT();
            int blockCounter = 1;
            BlockID prevBlockId = null;

            for (String line : lines) {
                BlockID blockId = new BlockID(siteId, blockCounter++);
                Block block = new Block(blockId, prevBlockId);

                CharID prevCharId = null;
                int charCounter = 1;
                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    CharID charId = new CharID(siteId, (blockCounter * 1000) + charCounter++);
                    CharNode node = new CharNode(charId, prevCharId, ch);
                    block.getContent().addChar(node);
                    prevCharId = charId;
                }
                importedDoc.addBlock(block);
                prevBlockId = blockId;
            }

            String docId = file.getName();
            if (docId.toLowerCase().endsWith(".txt")) {
                docId = docId.substring(0, docId.length() - 4);
            }

            notify("Imported '" + file.getName() + "' ("
                    + lines.size() + " lines, "
                    + countChars(importedDoc) + " characters).");

            if (actionListener != null) actionListener.onDocumentLoaded(docId, importedDoc);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent,
                    "Could not read file:\n" + e.getMessage(),
                    "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportFile(BlockCRDT doc) {
        if (doc == null) {
            notify("No document to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export as .txt");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        int result = chooser.showSaveDialog(parentComponent);
        if (result != JFileChooser.APPROVE_OPTION) {
            notify("Export cancelled.");
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getAbsolutePath() + ".txt");
        }

        String plainText = buildPlainText(doc);

        try {
            Files.writeString(file.toPath(), plainText, StandardCharsets.UTF_8);
            notify("Exported to '" + file.getName() + "' successfully.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent,
                    "Could not write file:\n" + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String renameDocument(String currentDocId) {
        String newName = JOptionPane.showInputDialog(
                parentComponent,
                "New name for document '" + currentDocId + "':",
                "Rename Document",
                JOptionPane.PLAIN_MESSAGE);

        if (newName == null || newName.trim().isEmpty()) {
            notify("Rename cancelled.");
            return null;
        }
        newName = newName.trim();
        try {
            String url = getServerBase() + "/api/documents?docId="
                    + java.net.URLEncoder.encode(newName, StandardCharsets.UTF_8)
                    + "&name="
                    + java.net.URLEncoder.encode(newName, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                notify("Document renamed to '" + newName + "' and saved.");
            } else {
                notify("Renamed to '" + newName + "' locally. Server responded: "
                        + response.statusCode());
            }
        } catch (Exception e) {
            notify("Renamed to '" + newName + "' (no server connection: " + e.getMessage() + ").");
        }

        return newName;
    }

    public boolean confirmDeleteDocument(String docId) {
        int choice = JOptionPane.showConfirmDialog(
                parentComponent,
                "Are you sure you want to delete '" + docId + "'?\n",
                "Delete Document",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            notify("Delete cancelled.");
            return false;
        }
        try {
            String url = getServerBase() + "/api/documents/"
                    + java.net.URLEncoder.encode(docId, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                notify("Document '" + docId + "' deleted from server.");
            } else if (response.statusCode() == 404) {
                notify("Document '" + docId + "' was not on the server (may not have been saved yet).");
            } else {
                notify("Server responded " + response.statusCode()
                        + " — removing locally anyway.");
            }
        } catch (Exception e) {
            notify("Deleted locally (no server connection: " + e.getMessage() + ").");
        }

        return true;
    }

    private String buildPlainText(BlockCRDT doc) {
        StringBuilder sb = new StringBuilder();
        boolean firstBlock = true;
        for (Block block : doc.allBlocks) {
            if (block.checkDeleted()) continue;
            if (!firstBlock) sb.append("\n");
            firstBlock = false;
            for (CharNode node : block.getContent().allNodes) {
                if (!node.checkDeleted()) {
                    sb.append(node.getMyChar());
                }
            }
        }
        return sb.toString();
    }

    private int countChars(BlockCRDT doc) {
        int total = 0;
        for (Block block : doc.allBlocks) {
            if (block.checkDeleted()) continue;
            total += block.getContent().getLength();
        }
        return total;
    }

    private void notify(String msg) {
        if (actionListener != null) actionListener.onStatusMessage(msg);
        else System.out.println("[FileManager] " + msg);
    }

    private String getServerBase() {
        return serverBaseSupplier.get();
    }

    public void openDocument() {
        String serverBase = getServerBase();
        new Thread(() -> {
            try {
                String url = serverBase + "/api/documents";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        http.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(parentComponent,
                                    "Server error: " + response.statusCode(),
                                    "Open Failed", JOptionPane.ERROR_MESSAGE));
                    return;
                }

                // Parse the JSON array of documents manually
                String body = response.body();
                java.util.List<String[]> docs = parseDocumentList(body);

                if (docs.isEmpty()) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(parentComponent,
                                    "No saved documents found on server.",
                                    "Open Document", JOptionPane.INFORMATION_MESSAGE));
                    return;
                }

                SwingUtilities.invokeLater(() -> showOpenDialog(docs));

            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parentComponent,
                                "Cannot reach server:\n" + e.getMessage(),
                                "Open Failed", JOptionPane.ERROR_MESSAGE));
            }
        }, "open-doc-fetch").start();
    }

    private java.util.List<String[]> parseDocumentList(String json) {
        java.util.List<String[]> result = new java.util.ArrayList<>();
        // Each entry looks like: {"id":"...","name":"...","updatedAt":"..."}
        int idx = 0;
        while (true) {
            int start = json.indexOf('{', idx);
            if (start < 0) break;
            int end = json.indexOf('}', start);
            if (end < 0) break;
            String entry = json.substring(start, end + 1);

            String id        = extractJson(entry, "id");
            String name      = extractJson(entry, "name");
            String updatedAt = extractJson(entry, "updatedAt");

            if (!id.isEmpty()) {
                result.add(new String[]{id, name.isEmpty() ? id : name, updatedAt});
            }
            idx = end + 1;
        }
        return result;
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf('"', start);
        return end < 0 ? "" : json.substring(start, end);
    }

    private void showOpenDialog(java.util.List<String[]> docs) {
        // Build display strings for the list
        String[] displayNames = new String[docs.size()];
        for (int i = 0; i < docs.size(); i++) {
            String name      = docs.get(i)[1];
            String updatedAt = docs.get(i)[2];
            // Format: "MyDoc  (saved: 2026-05-04T10:30:00)"
            displayNames[i] = updatedAt.isEmpty()
                    ? name
                    : name + "  (saved: " + updatedAt.replace("T", " ") + ")";
        }

        JList<String> list = new JList<>(displayNames);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFont(new Font("Arial", Font.PLAIN, 13));
        list.setVisibleRowCount(Math.min(docs.size(), 8));

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(420, 200));

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel("Select a document to open:"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(
                parentComponent, panel,
                "Open Document", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            notify("Open cancelled.");
            return;
        }

        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex < 0) {
            notify("No document selected.");
            return;
        }

        String selectedId = docs.get(selectedIndex)[0];
        loadDocumentFromServer(selectedId);
    }

    private void loadDocumentFromServer(String docId) {
        notify("Loading '" + docId + "' from server...");
        String serverBase = getServerBase();
        new Thread(() -> {
            try {
                String url = serverBase + "/api/documents/"
                        + java.net.URLEncoder.encode(docId, java.nio.charset.StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        http.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse the blocks field out of the response
                    String body = response.body();
                    com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT loadedDoc = parseBlocksFromResponse(body);

                    SwingUtilities.invokeLater(() -> {
                        notify("Document '" + docId + "' loaded — click Connect to join.");
                        if (actionListener != null) {
                            actionListener.onDocumentLoaded(docId, loadedDoc);
                        }
                    });

                } else if (response.statusCode() == 404) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(parentComponent,
                                    "Document '" + docId + "' not found on server.",
                                    "Open Failed", JOptionPane.ERROR_MESSAGE));
                } else {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(parentComponent,
                                    "Server error: " + response.statusCode(),
                                    "Open Failed", JOptionPane.ERROR_MESSAGE));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parentComponent,
                                "Cannot reach server:\n" + e.getMessage(),
                                "Open Failed", JOptionPane.ERROR_MESSAGE));
            }
        }, "open-doc-load").start();
    }

    private com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT parseBlocksFromResponse(String body) {
        com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT doc =
                new com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT();
        try {
            // Extract the "blocks" string value from the JSON response
            String blocksKey = "\"blocks\":\"";
            int start = body.indexOf(blocksKey);
            if (start < 0) return doc; // no blocks field, return empty

            start += blocksKey.length();

            // The blocks value is a JSON-encoded string, find its end
            // It was serialized with toString() so it's wrapped in quotes and escaped
            // We need to extract and unescape it
            StringBuilder sb = new StringBuilder();
            int i = start;
            while (i < body.length()) {
                char c = body.charAt(i);
                if (c == '\\' && i + 1 < body.length()) {
                    char next = body.charAt(i + 1);
                    if (next == '"')       { sb.append('"');  i += 2; continue; }
                    if (next == '\\')      { sb.append('\\'); i += 2; continue; }
                    if (next == 'n')       { sb.append('\n'); i += 2; continue; }
                    if (next == 't')       { sb.append('\t'); i += 2; continue; }
                }
                if (c == '"') break; // end of the string value
                sb.append(c);
                i++;
            }

            String blocksJson = sb.toString();
            org.json.JSONArray blocks = new org.json.JSONArray(blocksJson);

            for (int b = 0; b < blocks.length(); b++) {
                org.json.JSONObject blockJson = blocks.getJSONObject(b);

                com.Team2_CDE_master.ProjectServer.crdt.BlockID blockId =
                        new com.Team2_CDE_master.ProjectServer.crdt.BlockID(
                                blockJson.getInt("siteId"),
                                blockJson.getInt("counter"));

                com.Team2_CDE_master.ProjectServer.crdt.BlockID parentId = null;
                if (!blockJson.isNull("parentId")) {
                    org.json.JSONObject p = blockJson.getJSONObject("parentId");
                    parentId = new com.Team2_CDE_master.ProjectServer.crdt.BlockID(
                            p.getInt("siteId"), p.getInt("counter"));
                }

                com.Team2_CDE_master.ProjectServer.crdt.Block block =
                        new com.Team2_CDE_master.ProjectServer.crdt.Block(blockId, parentId);

                org.json.JSONArray chars = blockJson.getJSONArray("chars");
                for (int c = 0; c < chars.length(); c++) {
                    org.json.JSONObject charJson = chars.getJSONObject(c);

                    com.Team2_CDE_master.ProjectServer.crdt.CharID charId =
                            new com.Team2_CDE_master.ProjectServer.crdt.CharID(
                                    charJson.getInt("siteId"),
                                    charJson.getInt("myNum"));

                    com.Team2_CDE_master.ProjectServer.crdt.CharID charParent = null;
                    if (!charJson.isNull("parentId")) {
                        org.json.JSONObject cp = charJson.getJSONObject("parentId");
                        charParent = new com.Team2_CDE_master.ProjectServer.crdt.CharID(
                                cp.getInt("siteId"), cp.getInt("myNum"));
                    }

                    char ch = charJson.getString("char").charAt(0);
                    com.Team2_CDE_master.ProjectServer.crdt.CharNode node =
                            new com.Team2_CDE_master.ProjectServer.crdt.CharNode(charId, charParent, ch);

                    if (charJson.getBoolean("deleted")) node.markDeleted();
                    node.setBold(charJson.getBoolean("bold"));
                    node.setItalic(charJson.getBoolean("italic"));

                    block.getContent().addChar(node);
                }

                doc.addBlock(block);
            }

        } catch (Exception e) {
            System.err.println("[FileManager] Failed to parse blocks: " + e.getMessage());
        }
        return doc;
    }
}
