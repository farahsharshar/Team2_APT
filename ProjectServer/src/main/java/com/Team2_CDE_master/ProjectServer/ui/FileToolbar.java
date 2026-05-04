package com.Team2_CDE_master.ProjectServer.ui;

import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;

import javax.swing.*;
import java.awt.*;
//rovana
public class FileToolbar extends JPanel {

    // Callback interface — parent window (EditorWindow) implements this
    public interface FileToolbarListener {
        //on... better than do... (clean arch)
        /*create or import-> send docid & CRDT ll EditorWindow*/
        void onDocumentLoaded(String docId, BlockCRDT doc);

        /*rename to newName(string) */
        void onRenameRequested(String newName);

        /*delete l current or clear editor */
        void onDeleteRequested();

        /*Status msgs(done .. successful) */
        void onStatusMessage(String message);

        /* Export */
        BlockCRDT getCurrentDocument();

        /* Rename & Delete & connection-> can reach directly l EditorWindow*/
        String getCurrentDocId();
    }
    // Fields
    private final FileManager fileManager;
    private FileToolbarListener toolbarListener;

    // Constructor l toolbar
    public FileToolbar(Component parentComponent, int siteId) {
        fileManager = new FileManager(parentComponent);
        fileManager.setSiteId(siteId);

        // connect FileManager with toolbar (vv imp)
        //nested callback
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

    // Setup
    public void setToolbarListener(FileToolbarListener listener) {
        this.toolbarListener = listener;
    }
    /* called by EditorWindow when the user changes the Site ID field and clicks Connect
     so that new/import operations use the correct site ID for generating CRDT identifiers */
    public void updateSiteId(int newSiteId) {
        fileManager.setSiteId(newSiteId);
    }

    // UI-Ux construction
    private void buildUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        setBorder(BorderFactory.createTitledBorder("File"));

        JButton newBtn    = new JButton("New");
        JButton importBtn = new JButton("Import .txt");
        JButton exportBtn = new JButton("Export .txt");
        JButton renameBtn = new JButton("Rename");
        JButton deleteBtn = new JButton("Delete");

        newBtn   .setToolTipText("Create a new empty document");
        importBtn.setToolTipText("Import a .txt file as a new document");
        exportBtn.setToolTipText("Export the current document to a .txt file");
        renameBtn.setToolTipText("Rename the current document");
        deleteBtn.setToolTipText("Delete the current document permanently");

        newBtn.addActionListener(e -> fileManager.createNewDocument());

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
        add(importBtn);
        add(exportBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(renameBtn);
        add(deleteBtn);
    }
}