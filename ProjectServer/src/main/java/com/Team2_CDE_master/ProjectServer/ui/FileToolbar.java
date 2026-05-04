package com.Team2_CDE_master.ProjectServer.ui;

import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class FileToolbar extends JPanel {

    public interface FileToolbarListener {
        void onDocumentLoaded(String docId, BlockCRDT doc);
        void onRenameRequested(String newName);
        void onDeleteRequested();
        void onStatusMessage(String message);
        BlockCRDT getCurrentDocument();
        String getCurrentDocId();
    }

    private final FileManager fileManager;
    private FileToolbarListener toolbarListener;

    public FileToolbar(Component parentComponent, int siteId, Supplier<String> serverBaseSupplier) {
        fileManager = new FileManager(parentComponent, serverBaseSupplier);
        fileManager.setSiteId(siteId);

        fileManager.setFileActionListener(new FileManager.FileActionListener() {
            @Override
            public void onDocumentLoaded(String docId, BlockCRDT doc) {
                if (toolbarListener != null)
                    toolbarListener.onDocumentLoaded(docId, doc);
            }

            @Override
            public void onStatusMessage(String message) {
                if (toolbarListener != null)
                    toolbarListener.onStatusMessage(message);
            }
        });

        buildUI();
    }

    public void setToolbarListener(FileToolbarListener listener) {
        this.toolbarListener = listener;
    }

    public void updateSiteId(int newSiteId) {
        fileManager.setSiteId(newSiteId);
    }

    private void buildUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        setBorder(BorderFactory.createTitledBorder("File"));

        JButton newBtn    = new JButton("New");
        JButton openBtn   = new JButton("Open");
        JButton importBtn = new JButton("Import .txt");
        JButton exportBtn = new JButton("Export .txt");
        JButton renameBtn = new JButton("Rename");
        JButton deleteBtn = new JButton("Delete");

        newBtn   .setToolTipText("Create a new empty document");
        openBtn  .setToolTipText("Open a saved document from the server");
        importBtn.setToolTipText("Import a .txt file as a new document");
        exportBtn.setToolTipText("Export the current document to a .txt file");
        renameBtn.setToolTipText("Rename the current document");
        deleteBtn.setToolTipText("Delete the current document permanently");

        newBtn.addActionListener(e -> fileManager.createNewDocument());

        openBtn.addActionListener(e -> fileManager.openDocument());

        importBtn.addActionListener(e -> fileManager.importFile());

        exportBtn.addActionListener(e -> {
            if (toolbarListener == null) return;
            BlockCRDT doc = toolbarListener.getCurrentDocument();
            fileManager.exportFile(doc);
        });

        renameBtn.addActionListener(e -> {
            if (toolbarListener == null) return;
            String currentId = toolbarListener.getCurrentDocId();
            if (currentId == null || currentId.isEmpty()) {
                toolbarListener.onStatusMessage("No document is open.");
                return;
            }
            String newName = fileManager.renameDocument(currentId);
            if (newName != null) {
                toolbarListener.onRenameRequested(newName);
            }
        });

        deleteBtn.addActionListener(e -> {
            if (toolbarListener == null) return;
            String currentId = toolbarListener.getCurrentDocId();
            if (currentId == null || currentId.isEmpty()) {
                toolbarListener.onStatusMessage("No document is open.");
                return;
            }
            boolean confirmed = fileManager.confirmDeleteDocument(currentId);
            if (confirmed) {
                toolbarListener.onDeleteRequested();
            }
        });

        add(newBtn);
        add(openBtn);
        add(importBtn);
        add(exportBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(renameBtn);
        add(deleteBtn);
    }
}
