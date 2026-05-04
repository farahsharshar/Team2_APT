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

public class FileManager {

    public interface FileActionListener {
        void onDocumentLoaded(String docId, BlockCRDT doc);
        void onStatusMessage(String message);
    }

    private final Component parentComponent;
    private FileActionListener actionListener;
    private int siteId;

    private static final String SERVER_BASE = "http://localhost:8080";
    private final HttpClient http = HttpClient.newHttpClient();

    public FileManager(Component parentComponent) {
        this.parentComponent = parentComponent;
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
            String url = SERVER_BASE + "/api/documents?docId="
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
            String url = SERVER_BASE + "/api/documents/"
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
}
