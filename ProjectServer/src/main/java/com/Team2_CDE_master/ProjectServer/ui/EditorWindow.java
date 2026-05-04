package com.Team2_CDE_master.ProjectServer.ui;

import com.Team2_CDE_master.ProjectServer.client.OperationApplier;
import com.Team2_CDE_master.ProjectServer.client.UndoRedoManager;
import com.Team2_CDE_master.ProjectServer.client.UserPresenceManager;
import com.Team2_CDE_master.ProjectServer.crdt.*;
import com.Team2_CDE_master.ProjectServer.network.NetworkManager;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class EditorWindow extends JFrame implements FileToolbar.FileToolbarListener {

    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 8081;

    private BlockCRDT localDoc = new BlockCRDT();
    private int siteId = 1;
    private BlockID currentBlockId;
    private String currentDocId = null;

    private JTextPane textPane;
    private JLabel statusLabel;
    private JTextField docIdField;
    private JTextField siteIdField;
    private JTextField serverHostField;
    private JButton connectBtn;
    private JButton boldBtn;
    private JButton italicBtn;
    // ELHEBEISHY'S PART
    private JButton undoBtn;
    private JButton redoBtn;
    private FileToolbar fileToolbar;
    private JButton shareBtn;
    private JLabel usersLabel;
    private JToggleButton viewerBtn;

    private final Map<Integer, Integer> remoteCursors = new LinkedHashMap<>();
    // ELHEBEISHY'S PART
    private final Map<Integer, Object> remoteCursorHighlights = new LinkedHashMap<>();
    private static final Color[] USER_COLORS = {
            new Color(200, 50,  50),
            new Color(50,  100, 200),
            new Color(30,  160, 30),
            new Color(200, 130, 0)
    };

    private boolean isUpdating = false;

    // ELHEBEISHY'S PART
    private boolean activeBold = false;
    private boolean activeItalic = false;

    private final UndoRedoManager undoManager = new UndoRedoManager();
    private final UserPresenceManager presenceManager = new UserPresenceManager();
    private OperationApplier applier;

    public EditorWindow() {
        super("Team 2 ");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
    }

    private void buildUI() {

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu("File"));
        menuBar.add(new JMenu("Edit"));
        menuBar.add(new JMenu("Insert"));
        menuBar.add(new JMenu("View"));
        setJMenuBar(menuBar);

        fileToolbar = new FileToolbar(this, siteId, this::getHttpBase);
        fileToolbar.setToolbarListener(this);

        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        docIdField  = new JTextField("myDoc", 10);
        siteIdField = new JTextField("1", 3);
        serverHostField = new JTextField(DEFAULT_SERVER_HOST, 12);
        connectBtn  = new JButton("Connect");

        connectPanel.add(new JLabel("Doc:"));
        connectPanel.add(docIdField);
        connectPanel.add(new JLabel("Site:"));
        connectPanel.add(siteIdField);
        connectPanel.add(new JLabel("Server IP:"));
        connectPanel.add(serverHostField);
        connectPanel.add(connectBtn);

        JButton joinBtn = new JButton("Join by Code");
        joinBtn.setToolTipText("Join a shared document using an editor or viewer code");
        joinBtn.addActionListener(e -> handleJoinByCode());
        connectPanel.add(joinBtn);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        boldBtn = new JButton("B");
        boldBtn.setFont(new Font("Arial", Font.BOLD, 14));
        boldBtn.setEnabled(false);
        boldBtn.setToolTipText("Bold");

        italicBtn = new JButton("I");
        italicBtn.setFont(new Font("Arial", Font.ITALIC, 14));
        italicBtn.setEnabled(false);
        italicBtn.setToolTipText("Italic");

        // ELHEBEISHY'S PART
        undoBtn = new JButton("Undo");
        undoBtn.setEnabled(false);
        undoBtn.setToolTipText("Undo");
        undoBtn.addActionListener(e -> handleUndo());

        // ELHEBEISHY'S PART
        redoBtn = new JButton("Redo");
        redoBtn.setEnabled(false);
        redoBtn.setToolTipText("Redo");
        redoBtn.addActionListener(e -> handleRedo());

        shareBtn = new JButton("🔗");
        shareBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        shareBtn.setToolTipText("Share document codes");
        shareBtn.setEnabled(false);
        shareBtn.addActionListener(e -> {
            if (ViewerMode.isViewer()) return;
            if (currentDocId != null) {
                ShareManager.showShareDialog(this, currentDocId, getHttpBase());
            }
        });

        usersLabel = new JLabel("👥 Only you");
        usersLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        usersLabel.setForeground(Color.DARK_GRAY);
        usersLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        viewerBtn = new JToggleButton("👁 Viewer");
        viewerBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        viewerBtn.setToolTipText("Your current role in this document");
        viewerBtn.setEnabled(false);
        viewerBtn.addActionListener(e -> {
            viewerBtn.setSelected(ViewerMode.isViewer());
            if (ViewerMode.isViewer()) {
                JOptionPane.showMessageDialog(this,
                        "You joined as a viewer.\nOnly editors can change roles.",
                        "Read-Only Mode", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // ELHEBEISHY'S PART
        toolbar.add(undoBtn);
        toolbar.add(redoBtn);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(boldBtn);
        toolbar.add(italicBtn);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(shareBtn);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(usersLabel);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(viewerBtn);

        textPane = new JTextPane();
        textPane.setFont(new Font("Arial", Font.PLAIN, 16));
        textPane.setMargin(new Insets(12, 12, 12, 12));
        // ELHEBEISHY'S PART
        textPane.setEditable(false);
        textPane.setEnabled(false);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Not connected");
        statusLabel.setForeground(Color.GRAY);
        bottomPanel.add(statusLabel);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(fileToolbar);
        topPanel.add(connectPanel);
        topPanel.add(toolbar);

        setLayout(new BorderLayout());
        add(topPanel,    BorderLayout.NORTH);
        add(scrollPane,  BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        connectBtn.addActionListener(e -> handleConnect());
        boldBtn.addActionListener(e -> handleFormatting("bold"));
        italicBtn.addActionListener(e -> handleFormatting("italic"));
        setupKeyListener();
        setupCaretListener();
    }

    @Override
    public void onDocumentLoaded(String docId, BlockCRDT doc) {
        ViewerMode.reset();

        if (NetworkManager.getInstance().isConnected()) {
            NetworkManager.getInstance().disconnect();
        }

        synchronized (this) {
            localDoc      = doc;
            currentDocId  = docId;
            currentBlockId = null;
        }

        SwingUtilities.invokeLater(() -> {
            docIdField.setText(docId);
            docIdField.setEnabled(true);
            siteIdField.setEnabled(true);
            serverHostField.setEnabled(true);
            connectBtn.setEnabled(true);

            textPane.setEnabled(false);
            // ELHEBEISHY'S PART
            textPane.setEditable(false);
            boldBtn.setEnabled(false);
            italicBtn.setEnabled(false);
            shareBtn.setEnabled(false);
            // ELHEBEISHY'S PART
            updateUndoRedoButtons();

            statusLabel.setText("Document '" + docId + "' ready — click Connect");
            statusLabel.setForeground(new Color(0, 100, 200));

            updateWindowTitle();
            refreshDisplay();
        });
    }

    @Override
    public void onRenameRequested(String newName) {
        SwingUtilities.invokeLater(() -> {
            currentDocId = newName;
            docIdField.setText(newName);
            updateWindowTitle();
            statusLabel.setText("Renamed to '" + newName + "'");
            statusLabel.setForeground(new Color(0, 130, 0));
        });
    }

    @Override
    public void onDeleteRequested() {
        if (NetworkManager.getInstance().isConnected()) {
            NetworkManager.getInstance().disconnect();
        }

        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                localDoc       = new BlockCRDT();
                currentDocId   = null;
                currentBlockId = null;
            }
            remoteCursors.clear();

            docIdField.setText("myDoc");
            docIdField.setEnabled(true);
            siteIdField.setEnabled(true);
            serverHostField.setEnabled(true);
            connectBtn.setEnabled(true);

            textPane.setEnabled(false);
            // ELHEBEISHY'S PART
            textPane.setEditable(false);
            boldBtn.setEnabled(false);
            italicBtn.setEnabled(false);
            shareBtn.setEnabled(false);
            // ELHEBEISHY'S PART
            updateUndoRedoButtons();

            statusLabel.setText("Document deleted — create or connect to a new one");
            statusLabel.setForeground(Color.GRAY);

            updateWindowTitle();
            refreshDisplay();
        });
    }

    @Override
    public void onStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(Color.DARK_GRAY);
        });
    }

    @Override
    public BlockCRDT getCurrentDocument() { return localDoc; }

    @Override
    public String getCurrentDocId() { return currentDocId; }

    private void updateWindowTitle() {
        if (currentDocId != null && !currentDocId.isEmpty()) {
            setTitle("Collaborative Text Editor — " + currentDocId + " — Team 2");
        } else {
            setTitle("Collaborative Text Editor — Team 2");
        }
    }

    private void handleJoinByCode() {
        JoinDialog.JoinResult result = JoinDialog.show(this, getHttpBase());
        if (result == null) return;

        ViewerMode.setRole(result.role());

        docIdField.setText(result.docId());

        applyRoleToUI();

        handleConnect();
    }

    private void applyRoleToUI() {
        boolean viewer = ViewerMode.isViewer();

        // ELHEBEISHY'S PART
        boolean canEdit = !viewer && NetworkManager.getInstance().isConnected();
        textPane.setEnabled(true);
        textPane.setEditable(canEdit);

        boldBtn.setEnabled(canEdit);
        italicBtn.setEnabled(canEdit);

        // Share button: visible and enabled only for editors, but only once connected
        shareBtn.setVisible(!viewer);
        shareBtn.setEnabled(!viewer && NetworkManager.getInstance().isConnected());

        viewerBtn.setSelected(viewer);
        viewerBtn.setText(ViewerMode.label());
        viewerBtn.setEnabled(false);

        // ELHEBEISHY'S PART
        updateUndoRedoButtons();
    }

    // ELHEBEISHY'S PART
    private void updateUndoRedoButtons() {
        boolean canUseHistory = !ViewerMode.isViewer() && NetworkManager.getInstance().isConnected();
        if (undoBtn != null) undoBtn.setEnabled(canUseHistory && undoManager.canUndo());
        if (redoBtn != null) redoBtn.setEnabled(canUseHistory && undoManager.canRedo());
    }

    private void handleConnect() {
        String docId = docIdField.getText().trim();
        if (docId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Doc ID");
            return;
        }
        try {
            siteId = Integer.parseInt(siteIdField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Site ID must be a number (1, 2, 3, or 4)");
            return;
        }

        fileToolbar.updateSiteId(siteId);

        currentDocId = docId;
        updateWindowTitle();

        this.applier = new OperationApplier(localDoc);

        applier.setOnDocumentChanged(() ->
                SwingUtilities.invokeLater(this::refreshDisplay));

        applier.setOnCursorUpdate((remoteSiteId, position) ->
                SwingUtilities.invokeLater(() -> {
                    remoteCursors.put(remoteSiteId, position);
                    // ELHEBEISHY'S PART
                    try {
                        drawRemoteCursors(textPane.getStyledDocument());
                    } catch (BadLocationException ex) {
                        System.err.println("[EditorWindow] Remote cursor draw failed: " + ex.getMessage());
                    }
                    updateUsersLabel();
                }));

        applier.setOnPresenceJoin((sid, uname) -> SwingUtilities.invokeLater(() -> {
            presenceManager.addUser(sid, uname);
            updateUsersLabel();
        }));

        applier.setOnPresenceLeave(sid -> SwingUtilities.invokeLater(() -> {
            presenceManager.removeUser(sid);
            updateUsersLabel();
        }));

        // ELHEBEISHY'S PART
        applier.setOnFullSyncApplied(() -> SwingUtilities.invokeLater(() -> {
            NetworkManager.getInstance().syncCountersFrom(localDoc);
            ensureEditorHasWritableBlock();
            applyRoleToUI();
            if (!ViewerMode.isViewer() && NetworkManager.getInstance().isConnected()) {
                textPane.requestFocusInWindow();
            }
        }));

        applier.setOnConnected(() -> SwingUtilities.invokeLater(() -> {
            currentBlockId = new BlockID(siteId, 1);
            if (localDoc.findBlock(currentBlockId) == null) {
                Block firstBlock = new Block(currentBlockId, null);
                synchronized (localDoc) { localDoc.addBlock(firstBlock); }
                NetworkManager.getInstance().sendInsertBlock(currentBlockId, null);
            }

            NetworkManager nm = NetworkManager.getInstance();
            if (nm.getReconnectionHandler().isDisconnected()) {
                nm.getReconnectionHandler().onReconnected(nm);
            }

            String roleLabel = ViewerMode.isViewer() ? " [READ-ONLY]" : "";
            statusLabel.setText("Connected — " + docId + "  (site " + siteId + ")" + roleLabel);
            statusLabel.setForeground(new Color(0, 130, 0));

            // ── Enable share button here, after confirmed connection ──────
            if (!ViewerMode.isViewer()) {
                shareBtn.setVisible(true);
                shareBtn.setEnabled(true);
            }

            applyRoleToUI();
            // ELHEBEISHY'S PART
            ensureEditorHasWritableBlock();
            // ELHEBEISHY'S PART
            NetworkManager.getInstance().syncCountersFrom(localDoc);

            viewerBtn.setEnabled(false);

            connectBtn.setEnabled(false);
            docIdField.setEnabled(false);
            siteIdField.setEnabled(false);
            serverHostField.setEnabled(false);
            textPane.requestFocus();
        }));

        applier.setOnDisconnected(() -> SwingUtilities.invokeLater(() -> {
            NetworkManager.getInstance().getReconnectionHandler().onDisconnected();
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.RED);
            textPane.setEnabled(false);
            // ELHEBEISHY'S PART
            textPane.setEditable(false);
            boldBtn.setEnabled(false);
            italicBtn.setEnabled(false);
            shareBtn.setEnabled(false);
            viewerBtn.setEnabled(false);
            // ELHEBEISHY'S PART
            updateUndoRedoButtons();
        }));

        String role = ViewerMode.isViewer() ? "VIEWER" : "EDITOR";
        NetworkManager.getInstance().connect(getWsBase(), docId, siteId, applier, role);

        statusLabel.setText("Connecting...");
        statusLabel.setForeground(Color.ORANGE);
    }

    private String getServerHost() {
        if (serverHostField == null) return DEFAULT_SERVER_HOST;
        String host = serverHostField.getText().trim();
        if (host.isEmpty()) return DEFAULT_SERVER_HOST;

        host = host.replaceFirst("(?i)^(https?://|wss?://)", "");
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        if (host.endsWith(":" + SERVER_PORT)) host = host.substring(0, host.length() - (":" + SERVER_PORT).length());
        return host.isBlank() ? DEFAULT_SERVER_HOST : host;
    }

    private String getHttpBase() {
        return "http://" + getServerHost() + ":" + SERVER_PORT;
    }

    private String getWsBase() {
        return "ws://" + getServerHost() + ":" + SERVER_PORT;
    }

    // ELHEBEISHY'S PART
    private void ensureEditorHasWritableBlock() {
        if (ViewerMode.isViewer() || !NetworkManager.getInstance().isConnected()) return;

        BlockID blockToSend = null;
        synchronized (localDoc) {
            for (Block block : localDoc.allBlocks) {
                if (!block.checkDeleted()) {
                    currentBlockId = block.getMyId();
                    return;
                }
            }

            // ELHEBEISHY'S PART
            blockToSend = currentBlockId != null
                    ? currentBlockId
                    : NetworkManager.getInstance().generateBlockID();
            localDoc.addBlock(new Block(blockToSend, null));
            currentBlockId = blockToSend;
        }

        NetworkManager.getInstance().sendInsertBlock(blockToSend, null);
        refreshDisplay();
    }

    private void setupKeyListener() {
        textPane.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                if (isUpdating || !NetworkManager.getInstance().isConnected()) return;
                char ch = e.getKeyChar();
                if (ch >= 32 && ch < 127) {
                    e.consume();
                    handleInsertChar(ch);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (isUpdating || !NetworkManager.getInstance().isConnected()) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE -> { e.consume(); handleBackspace(); }
                    case KeyEvent.VK_ENTER      -> { e.consume(); handleEnter(); }
                    case KeyEvent.VK_DELETE     -> { e.consume(); handleDeleteForward(); }
                    case KeyEvent.VK_Z -> {
                        // ELHEBEISHY'S PART
                        if (e.isControlDown() && e.isShiftDown()) { e.consume(); handleRedo(); }
                        else if (e.isControlDown()) { e.consume(); handleUndo(); }
                    }
                    case KeyEvent.VK_Y -> {
                        if (e.isControlDown()) { e.consume(); handleRedo(); }
                    }
                    case KeyEvent.VK_V -> {
                        if (e.isControlDown()) { e.consume(); handlePaste(); }
                    }
                    case KeyEvent.VK_X -> {
                        // ELHEBEISHY'S PART
                        if (e.isControlDown()) { e.consume(); handleCut(); }
                    }
                }
            }
        });
    }

    private void setupCaretListener() {
        textPane.addCaretListener(e -> {
            if (!isUpdating && NetworkManager.getInstance().isConnected()) {
                sendCursorUpdate(e.getDot());
                // ELHEBEISHY'S PART
                updateActiveFormattingFromCaret();
            }
        });
    }

    private void handleUndo() {
        if (ViewerMode.isViewer()) return;
        if (!NetworkManager.getInstance().isConnected()) return;
        // ELHEBEISHY'S PART
        java.util.List<String> inverseOps = undoManager.undoOperations();
        if (inverseOps.isEmpty()) {
            // ELHEBEISHY'S PART
            setEditorCaretAndFocus(textPane.getCaretPosition());
            updateUndoRedoButtons();
            return;
        }
        applyAndSendOperations(inverseOps, "Undo");
    }

    private void handleRedo() {
        if (ViewerMode.isViewer()) return;
        if (!NetworkManager.getInstance().isConnected()) return;
        // ELHEBEISHY'S PART
        java.util.List<String> originalOps = undoManager.redoOperations();
        if (originalOps.isEmpty()) {
            // ELHEBEISHY'S PART
            setEditorCaretAndFocus(textPane.getCaretPosition());
            updateUndoRedoButtons();
            return;
        }
        applyAndSendOperations(originalOps, "Redo");
    }

    // ELHEBEISHY'S PART
    private void applyAndSendOperations(java.util.List<String> operations, String label) {
        try {
            // ELHEBEISHY'S PART
            int caretAfterInsert = -1;
            for (String json : operations) {
                // ELHEBEISHY'S PART
                org.json.JSONObject op = new org.json.JSONObject(json);
                applyLocalOp(op);
                NetworkManager.getInstance().sendRawMessage(json);
                if ("insert_char".equals(op.optString("type"))) {
                    caretAfterInsert = findCaretAfterInsertedChar(op);
                }
            }
            refreshDisplay();
            // ELHEBEISHY'S PART
            if (caretAfterInsert >= 0) {
                setEditorCaretAndFocus(caretAfterInsert);
            } else {
                setEditorCaretAndFocus(textPane.getCaretPosition());
            }
            // ELHEBEISHY'S PART
            updateUndoRedoButtons();
        } catch (Exception ex) {
            System.err.println("[" + label + "] " + ex.getMessage());
        }
    }

    // ELHEBEISHY'S PART
    private int findCaretAfterInsertedChar(org.json.JSONObject op) {
        CharID target = new CharID(
                op.getJSONObject("charId").getInt("siteId"),
                op.getJSONObject("charId").getInt("myNum"));
        int offset = 0;
        synchronized (localDoc) {
            for (Block block : localDoc.allBlocks) {
                if (block.checkDeleted()) continue;
                for (CharNode node : block.getContent().allNodes) {
                    if (node.checkDeleted()) continue;
                    if (node.getMyId().isSameAs(target)) {
                        return offset + 1;
                    }
                    offset++;
                }
                offset++;
            }
        }
        return -1;
    }

    // ELHEBEISHY'S PART
    private void handleCut() {
        if (ViewerMode.isViewer()) return;
        if (!NetworkManager.getInstance().isConnected()) return;
        if (!hasSelection()) return;

        String selectedText = textPane.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            try {
                java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(selectedText), null);
            } catch (Exception ex) {
                System.err.println("[Cut] Clipboard update failed: " + ex.getMessage());
            }
        }

        handleSelectedRangeDelete();
    }

    private void handlePaste() {
        if (ViewerMode.isViewer()) return;
        if (!NetworkManager.getInstance().isConnected()) return;
        try {
            String txt = (String) java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            if (txt == null || txt.isEmpty()) return;
            for (int i = 0; i < txt.length(); i++) {
                char c = txt.charAt(i);
                if (c == '\n') handleEnter();
                else if (c >= 32 && c < 127) handleInsertChar(c);
            }
        } catch (Exception ex) {
            System.err.println("[Paste] " + ex.getMessage());
        }
    }

    private void applyLocalOp(org.json.JSONObject j) {
        String type = j.getString("type");
        synchronized (localDoc) {
            switch (type) {
                case "insert_char" -> {
                    Block b = findBlockStr(j.getString("blockId"));
                    if (b == null) return;
                    CharID cid = new CharID(j.getJSONObject("charId").getInt("siteId"), j.getJSONObject("charId").getInt("myNum"));
                    CharID pid = j.isNull("parentId") ? null : new CharID(j.getJSONObject("parentId").getInt("siteId"), j.getJSONObject("parentId").getInt("myNum"));
                    // ELHEBEISHY'S PART
                    CharNode node = new CharNode(cid, pid, j.getString("char").charAt(0));
                    node.setBold(j.optBoolean("bold", false));
                    node.setItalic(j.optBoolean("italic", false));
                    b.getContent().addChar(node);
                }
                case "delete_char" -> {
                    Block b = findBlockStr(j.getString("blockId"));
                    if (b == null) return;
                    CharID cid = new CharID(j.getJSONObject("charId").getInt("siteId"), j.getJSONObject("charId").getInt("myNum"));
                    CharNode n = b.getContent().findNode(cid);
                    if (n != null) n.markDeleted();
                }
                case "insert_block" -> {
                    BlockID bid = new BlockID(j.getJSONObject("blockId").getInt("siteId"), j.getJSONObject("blockId").getInt("counter"));
                    BlockID par = j.isNull("parentBlockId") ? null : new BlockID(j.getJSONObject("parentBlockId").getInt("siteId"), j.getJSONObject("parentBlockId").getInt("counter"));
                    // ELHEBEISHY'S PART
                    localDoc.addBlock(new Block(bid, par));
                }
                case "delete_block" -> {
                    BlockID bid = new BlockID(j.getJSONObject("blockId").getInt("siteId"), j.getJSONObject("blockId").getInt("counter"));
                    localDoc.deleteBlock(bid);
                }
                case "split_block" -> {
                    // ELHEBEISHY'S PART
                    BlockID target = blockIdFromJson(j.getJSONObject("targetBlockId"));
                    BlockID newBlock = blockIdFromJson(j.getJSONObject("newBlockId"));
                    localDoc.splitBlock(target, j.getInt("splitIndex"), newBlock);
                    currentBlockId = newBlock;
                }
                case "merge_blocks" -> {
                    // ELHEBEISHY'S PART
                    BlockID first = blockIdFromJson(j.getJSONObject("firstBlockId"));
                    BlockID second = blockIdFromJson(j.getJSONObject("secondBlockId"));
                    localDoc.mergeBlocks(first, second);
                    currentBlockId = first;
                }
                case "formatting" -> {
                    Block b = findBlockStr(j.getString("blockId"));
                    if (b == null) return;
                    CharID cid = new CharID(j.getJSONObject("charId").getInt("siteId"), j.getJSONObject("charId").getInt("myNum"));
                    b.getContent().applyFormatting(cid, j.getString("formatType"), j.getBoolean("value"));
                }
                default -> {}
            }
        }
    }

    // ELHEBEISHY'S PART
    private BlockID blockIdFromJson(org.json.JSONObject json) {
        return new BlockID(json.getInt("siteId"), json.getInt("counter"));
    }

    private Block findBlockStr(String s) {
        String[] p = s.replace("B", "").split("_");
        return localDoc.findBlock(new BlockID(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
    }

    // ELHEBEISHY'S PART
    private static final class SelectedDeleteResult {
        private final int start;
        private final java.util.List<String> deleteOps;
        private final java.util.List<String> restoreOps;

        private SelectedDeleteResult(int start, java.util.List<String> deleteOps, java.util.List<String> restoreOps) {
            this.start = start;
            this.deleteOps = deleteOps;
            this.restoreOps = restoreOps;
        }
    }

    // ELHEBEISHY'S PART
    private boolean hasSelection() {
        return textPane.getSelectionStart() != textPane.getSelectionEnd();
    }

    // ELHEBEISHY'S PART
    private void handleSelectedRangeDelete() {
        // ELHEBEISHY'S PART
        SelectedDeleteResult deletion = deleteSelectedRangeFromDocument();
        if (deletion == null) return;

        // ELHEBEISHY'S PART
        sendRawOperations(deletion.deleteOps);
        undoManager.recordGroup(deletion.deleteOps, deletion.restoreOps);
        updateUndoRedoButtons();

        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(deletion.start);
    }

    // ELHEBEISHY'S PART
    private SelectedDeleteResult deleteSelectedRangeFromDocument() {
        int start = Math.min(textPane.getSelectionStart(), textPane.getSelectionEnd());
        int end = Math.max(textPane.getSelectionStart(), textPane.getSelectionEnd());
        if (start == end) return null;

        java.util.List<String> deleteOps = new ArrayList<>();
        java.util.List<String> restoreOps = new ArrayList<>();

        synchronized (localDoc) {
            int offset = 0;
            for (Block block : localDoc.allBlocks) {
                if (block.checkDeleted()) continue;
                String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

                for (CharNode node : block.getContent().allNodes) {
                    if (node.checkDeleted()) continue;
                    if (offset >= start && offset < end) {
                        deleteOps.add(com.Team2_CDE_master.ProjectServer.network.OperationSerializer.deleteChar(
                                blockIdStr, node.getMyId()));
                        restoreOps.add(com.Team2_CDE_master.ProjectServer.network.OperationSerializer.insertChar(
                                blockIdStr, node.getMyId(), node.getParentId(), node.getMyChar(),
                                node.checkBold(), node.checkItalic()));
                        node.markDeleted();
                    }
                    offset++;
                }
                offset++;
            }
        }

        if (deleteOps.isEmpty()) return null;
        return new SelectedDeleteResult(start, deleteOps, restoreOps);
    }

    // ELHEBEISHY'S PART
    private void sendRawOperations(java.util.List<String> operations) {
        for (String operation : operations) {
            NetworkManager.getInstance().sendRawMessage(operation);
        }
    }

    // ELHEBEISHY'S PART
    private void handleReplaceSelectionWithChar(char ch) {
        SelectedDeleteResult deletion = deleteSelectedRangeFromDocument();
        if (deletion == null) return;

        // ELHEBEISHY'S PART
        boolean keepBold = activeBold;
        boolean keepItalic = activeItalic;

        sendRawOperations(deletion.deleteOps);
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(deletion.start);
        activeBold = keepBold;
        activeItalic = keepItalic;
        updateFormattingButtons();

        String[] insertedOps = insertCharAtCaret(ch, false);
        if (insertedOps == null) {
            undoManager.recordGroup(deletion.deleteOps, deletion.restoreOps);
            updateUndoRedoButtons();
            refreshDisplay();
            // ELHEBEISHY'S PART
            setEditorCaretAndFocus(deletion.start);
            return;
        }

        java.util.List<String> originalOps = new ArrayList<>(deletion.deleteOps);
        originalOps.add(insertedOps[0]);

        java.util.List<String> inverseOps = new ArrayList<>();
        inverseOps.add(insertedOps[1]);
        inverseOps.addAll(deletion.restoreOps);

        undoManager.recordGroup(originalOps, inverseOps);
        updateUndoRedoButtons();
    }

    private void handleInsertChar(char ch) {
        if (ViewerMode.isViewer()) return;

        // ELHEBEISHY'S PART
        if (hasSelection()) {
            handleReplaceSelectionWithChar(ch);
            return;
        }

        // ELHEBEISHY'S PART
        insertCharAtCaret(ch, true);
    }

    // ELHEBEISHY'S PART
    private String[] insertCharAtCaret(char ch, boolean recordHistory) {
        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return null;

        Block block      = (Block) blockAndIndex[0];
        int   localIndex = (Integer) blockAndIndex[1];

        CharNode parentNode = getNodeAtVisiblePos(block, localIndex - 1);
        CharID   parentId   = (parentNode != null) ? parentNode.getMyId() : null;

        CharID newCharId  = NetworkManager.getInstance().generateCharID();
        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

        // ELHEBEISHY'S PART
        boolean insertBold = activeBold;
        boolean insertItalic = activeItalic;

        synchronized (localDoc) {
            CharNode newNode = new CharNode(newCharId, parentId, ch);
            // ELHEBEISHY'S PART
            newNode.setBold(insertBold);
            newNode.setItalic(insertItalic);
            block.getContent().addChar(newNode);
        }

        // ELHEBEISHY'S PART
        String orig = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.insertChar(
                blockIdStr, newCharId, parentId, ch, insertBold, insertItalic);
        String undo = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.deleteChar(blockIdStr, newCharId);
        NetworkManager.getInstance().sendRawMessage(orig);

        if (recordHistory) {
            // ELHEBEISHY'S PART
            undoManager.record(orig, undo);
            updateUndoRedoButtons();
        }

        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(caretPos + 1);
        return new String[]{orig, undo};
    }

    private void handleBackspace() {
        if (ViewerMode.isViewer()) return;

        // ELHEBEISHY'S PART
        if (hasSelection()) {
            handleSelectedRangeDelete();
            return;
        }

        int caretPos = textPane.getCaretPosition();
        if (caretPos == 0) return;

        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block      = (Block) blockAndIndex[0];
        int   localIndex = (Integer) blockAndIndex[1];

        if (localIndex == 0) {
            handleMergeWithPrevious(block);
            return;
        }

        CharNode toDelete = getNodeAtVisiblePos(block, localIndex - 1);
        if (toDelete == null) return;

        char savedChar = toDelete.getMyChar();
        CharID savedParent = toDelete.getParentId();
        // ELHEBEISHY'S PART
        boolean savedBold = toDelete.checkBold();
        boolean savedItalic = toDelete.checkItalic();

        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

        synchronized (localDoc) { toDelete.markDeleted(); }
        NetworkManager.getInstance().sendDeleteChar(blockIdStr, toDelete.getMyId());

        String del = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.deleteChar(blockIdStr, toDelete.getMyId());
        // ELHEBEISHY'S PART
        String ins = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.insertChar(
                blockIdStr, toDelete.getMyId(), savedParent, savedChar, savedBold, savedItalic);
        undoManager.record(del, ins);
        // ELHEBEISHY'S PART
        updateUndoRedoButtons();

        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(caretPos - 1);
    }

    private void handleDeleteForward() {
        if (ViewerMode.isViewer()) return;

        // ELHEBEISHY'S PART
        if (hasSelection()) {
            handleSelectedRangeDelete();
            return;
        }

        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block      = (Block) blockAndIndex[0];
        int   localIndex = (Integer) blockAndIndex[1];
        int   blockLen   = block.getContent().getLength();

        if (localIndex >= blockLen) {
            handleMergeWithNext(block);
            return;
        }

        CharNode toDelete = getNodeAtVisiblePos(block, localIndex);
        if (toDelete == null) return;

        // ELHEBEISHY'S PART
        char savedChar = toDelete.getMyChar();
        CharID savedParent = toDelete.getParentId();
        boolean savedBold = toDelete.checkBold();
        boolean savedItalic = toDelete.checkItalic();

        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());
        synchronized (localDoc) { toDelete.markDeleted(); }
        NetworkManager.getInstance().sendDeleteChar(blockIdStr, toDelete.getMyId());

        // ELHEBEISHY'S PART
        String del = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.deleteChar(blockIdStr, toDelete.getMyId());
        String ins = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.insertChar(
                blockIdStr, toDelete.getMyId(), savedParent, savedChar, savedBold, savedItalic);
        undoManager.record(del, ins);
        // ELHEBEISHY'S PART
        updateUndoRedoButtons();

        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(caretPos);
    }

    private void handleEnter() {
        if (ViewerMode.isViewer()) return;

        // ELHEBEISHY'S PART
        boolean keepBold = activeBold;
        boolean keepItalic = activeItalic;

        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block      = (Block) blockAndIndex[0];
        int   localIndex = (Integer) blockAndIndex[1];
        int   blockLen   = block.getContent().getLength();

        BlockID newBlockId = NetworkManager.getInstance().generateBlockID();
        // ELHEBEISHY'S PART
        String originalOp;
        String inverseOp;

        if (localIndex == 0 || localIndex >= blockLen) {
            synchronized (localDoc) {
                Block newBlock = new Block(newBlockId, block.getMyId());
                localDoc.addBlock(newBlock);
                currentBlockId = newBlockId;
            }
            // ELHEBEISHY'S PART
            originalOp = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.insertBlock(newBlockId, block.getMyId());
            inverseOp = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.deleteBlock(newBlockId);
        } else {
            synchronized (localDoc) {
                localDoc.splitBlock(block.getMyId(), localIndex, newBlockId);
                currentBlockId = newBlockId;
            }
            // ELHEBEISHY'S PART
            originalOp = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.splitBlock(block.getMyId(), localIndex, newBlockId);
            inverseOp = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.mergeBlocks(block.getMyId(), newBlockId);
        }

        // ELHEBEISHY'S PART
        NetworkManager.getInstance().sendRawMessage(originalOp);
        undoManager.record(originalOp, inverseOp);
        updateUndoRedoButtons();

        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(caretPos + 1);
        // ELHEBEISHY'S PART
        activeBold = keepBold;
        activeItalic = keepItalic;
        updateFormattingButtons();
    }

    private void handleMergeWithPrevious(Block block) {
        Block prevBlock = null;
        for (Block b : localDoc.allBlocks) {
            if (!b.checkDeleted() && !b.getMyId().isSameAs(block.getMyId())) {
                prevBlock = b;
            } else if (!b.checkDeleted() && b.getMyId().isSameAs(block.getMyId())) {
                break;
            }
        }
        if (prevBlock == null) return;

        int caretPos = textPane.getCaretPosition();
        synchronized (localDoc) {
            localDoc.mergeBlocks(prevBlock.getMyId(), block.getMyId());
            currentBlockId = prevBlock.getMyId();
        }
        NetworkManager.getInstance().sendMergeBlocks(prevBlock.getMyId(), block.getMyId());
        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(caretPos - 1);
    }

    private void handleMergeWithNext(Block block) {
        boolean foundCurrent = false;
        Block nextBlock = null;
        for (Block b : localDoc.allBlocks) {
            if (!b.checkDeleted()) {
                if (foundCurrent) { nextBlock = b; break; }
                if (b.getMyId().isSameAs(block.getMyId())) foundCurrent = true;
            }
        }
        if (nextBlock == null) return;

        int caretPos = textPane.getCaretPosition();
        synchronized (localDoc) {
            localDoc.mergeBlocks(block.getMyId(), nextBlock.getMyId());
        }
        NetworkManager.getInstance().sendMergeBlocks(block.getMyId(), nextBlock.getMyId());
        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(caretPos);
    }

    private void handleFormatting(String formatType) {
        if (ViewerMode.isViewer()) return;

        int start = textPane.getSelectionStart();
        int end   = textPane.getSelectionEnd();
        if (start == end) {
            // ELHEBEISHY'S PART
            toggleActiveFormatting(formatType);
            return;
        }

        // ELHEBEISHY'S PART
        boolean targetValue = getSelectionFormattingTarget(formatType, start, end);
        // ELHEBEISHY'S PART
        java.util.List<String> originalOps = new ArrayList<>();
        java.util.List<String> inverseOps = new ArrayList<>();

        int offset = 0;
        for (Block block : localDoc.allBlocks) {
            if (block.checkDeleted()) continue;
            String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

            for (CharNode node : block.getContent().allNodes) {
                if (node.checkDeleted()) continue;
                if (offset >= start && offset < end) {
                    // ELHEBEISHY'S PART
                    boolean newValue = targetValue;
                    boolean oldValue = nodeHasFormatting(node, formatType);
                    String formatOp = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.formatting(
                            blockIdStr, node.getMyId(), formatType, newValue);
                    String undoOp = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.formatting(
                            blockIdStr, node.getMyId(), formatType, oldValue);
                    synchronized (localDoc) {
                        block.getContent().applyFormatting(node.getMyId(), formatType, newValue);
                    }
                    NetworkManager.getInstance().sendRawMessage(formatOp);
                    originalOps.add(formatOp);
                    inverseOps.add(undoOp);
                }
                offset++;
            }
            offset++;
        }
        // ELHEBEISHY'S PART
        if (!originalOps.isEmpty()) {
            undoManager.recordGroup(originalOps, inverseOps);
            updateUndoRedoButtons();
        }
        refreshDisplay();
        // ELHEBEISHY'S PART
        setEditorCaretAndFocus(end);
        // ELHEBEISHY'S PART
        updateActiveFormattingFromCaret();
    }

    // ELHEBEISHY'S PART
    private boolean getSelectionFormattingTarget(String formatType, int start, int end) {
        boolean sawSelectedNode = false;
        boolean allSelectedAlreadyFormatted = true;

        synchronized (localDoc) {
            int offset = 0;
            for (Block block : localDoc.allBlocks) {
                if (block.checkDeleted()) continue;
                for (CharNode node : block.getContent().allNodes) {
                    if (node.checkDeleted()) continue;
                    if (offset >= start && offset < end) {
                        sawSelectedNode = true;
                        if (!nodeHasFormatting(node, formatType)) {
                            allSelectedAlreadyFormatted = false;
                        }
                    }
                    offset++;
                }
                offset++;
            }
        }

        return !sawSelectedNode || !allSelectedAlreadyFormatted;
    }

    // ELHEBEISHY'S PART
    private boolean nodeHasFormatting(CharNode node, String formatType) {
        return "bold".equals(formatType) ? node.checkBold() : node.checkItalic();
    }

    // ELHEBEISHY'S PART
    private void toggleActiveFormatting(String formatType) {
        if ("bold".equals(formatType)) {
            activeBold = !activeBold;
        } else if ("italic".equals(formatType)) {
            activeItalic = !activeItalic;
        }
        updateFormattingButtons();
        textPane.requestFocusInWindow();
        // ELHEBEISHY'S PART
        boolean keepBold = activeBold;
        boolean keepItalic = activeItalic;
        SwingUtilities.invokeLater(() -> {
            activeBold = keepBold;
            activeItalic = keepItalic;
            updateFormattingButtons();
            textPane.requestFocusInWindow();
        });
    }

    // ELHEBEISHY'S PART
    private void updateActiveFormattingFromCaret() {
        if (hasSelection()) return;

        CharNode previous = getVisibleNodeBeforeCaret(textPane.getCaretPosition());
        activeBold = previous != null && previous.checkBold();
        activeItalic = previous != null && previous.checkItalic();
        updateFormattingButtons();
    }

    // ELHEBEISHY'S PART
    private void updateFormattingButtons() {
        if (boldBtn != null) boldBtn.setSelected(activeBold);
        if (italicBtn != null) italicBtn.setSelected(activeItalic);
    }

    // ELHEBEISHY'S PART
    private void setEditorCaretAndFocus(int position) {
        int safePosition = Math.max(0, Math.min(position, getDocumentLength()));
        textPane.setCaretPosition(safePosition);
        if (!ViewerMode.isViewer() && textPane.isEditable()) {
            textPane.requestFocusInWindow();
        }
    }

    private void refreshDisplay() {
        isUpdating = true;
        int savedCaret = textPane.getCaretPosition();
        try {
            StyledDocument doc = textPane.getStyledDocument();
            doc.remove(0, doc.getLength());

            boolean firstBlock = true;
            for (Block block : localDoc.allBlocks) {
                if (block.checkDeleted()) continue;

                if (!firstBlock) {
                    doc.insertString(doc.getLength(), "\n", new SimpleAttributeSet());
                }
                firstBlock = false;

                for (CharNode node : block.getContent().allNodes) {
                    if (node.checkDeleted()) continue;
                    SimpleAttributeSet style = new SimpleAttributeSet();
                    if (node.checkBold())   StyleConstants.setBold(style, true);
                    if (node.checkItalic()) StyleConstants.setItalic(style, true);
                    StyleConstants.setFontFamily(style, "Arial");
                    StyleConstants.setFontSize(style, 16);
                    doc.insertString(doc.getLength(), String.valueOf(node.getMyChar()), style);
                }
            }

            drawRemoteCursors(doc);

        } catch (BadLocationException e) {
            System.err.println("[EditorWindow] Display refresh error: " + e.getMessage());
        } finally {
            // ELHEBEISHY'S PART
            int docLen = textPane.getDocument().getLength();
            textPane.setCaretPosition(Math.min(savedCaret, docLen));
            isUpdating = false;
        }
    }

    private void drawRemoteCursors(StyledDocument doc) throws BadLocationException {
        // ELHEBEISHY'S PART
        Highlighter highlighter = textPane.getHighlighter();
        for (Object tag : remoteCursorHighlights.values()) {
            highlighter.removeHighlight(tag);
        }
        remoteCursorHighlights.clear();

        // ELHEBEISHY'S PART
        int docLen = doc.getLength();
        if (docLen == 0) return;

        for (Map.Entry<Integer, Integer> entry : remoteCursors.entrySet()) {
            int remoteSiteId = entry.getKey();
            int position     = entry.getValue();
            if (remoteSiteId == siteId) continue;

            if (position > docLen) position = docLen;
            if (position < 0)     position = 0;

            Color color = USER_COLORS[remoteSiteId % USER_COLORS.length];
            // ELHEBEISHY'S PART
            int start = Math.min(position, docLen - 1);
            int end = Math.min(start + 1, docLen);
            Object tag = highlighter.addHighlight(
                    start,
                    end,
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90)));
            remoteCursorHighlights.put(remoteSiteId, tag);
        }
    }

    private void sendCursorUpdate(int caretPos) {
        if (!NetworkManager.getInstance().isConnected()) return;
        try {
            // ELHEBEISHY'S PART
            int safeCaret = Math.max(0, Math.min(caretPos, getDocumentLength()));
            NetworkManager.getInstance().sendCursorUpdate(siteId, safeCaret);
        } catch (Exception e) {
            System.err.println("[EditorWindow] Cursor update failed: " + e.getMessage());
        }
    }

    private void updateUsersLabel() {
        int others = presenceManager.getCount();
        if (others == 0) usersLabel.setText("👥 Only you");
        else usersLabel.setText("👥 You + " + others + " other" + (others > 1 ? "s" : ""));
    }

    private Object[] findBlockAtCaret(int caretPos) {
        int offset = 0;
        for (Block block : localDoc.allBlocks) {
            if (block.checkDeleted()) continue;
            int len = block.getContent().getLength();
            if (caretPos >= offset && caretPos <= offset + len) {
                return new Object[]{block, caretPos - offset};
            }
            offset += len + 1;
        }
        return null;
    }

    // ELHEBEISHY'S PART
    private CharNode getVisibleNodeBeforeCaret(int caretPos) {
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return null;

        Block block = (Block) blockAndIndex[0];
        int localIndex = (Integer) blockAndIndex[1];
        if (localIndex <= 0) return null;
        return getNodeAtVisiblePos(block, localIndex - 1);
    }

    private CharNode getNodeAtVisiblePos(Block block, int pos) {
        if (pos < 0) return null;
        int count = 0;
        for (CharNode node : block.getContent().allNodes) {
            if (!node.checkDeleted()) {
                if (count == pos) return node;
                count++;
            }
        }
        return null;
    }

    private int getDocumentLength() {
        return textPane.getDocument().getLength();
    }
}
