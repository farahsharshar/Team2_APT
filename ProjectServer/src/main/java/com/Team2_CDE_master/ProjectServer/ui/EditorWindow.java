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

    // ── Single server address used by both HTTP and WebSocket ──────────────
    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 8081;
    private static final String HTTP_BASE   = "http://" + SERVER_HOST + ":" + SERVER_PORT;
    private static final String WS_BASE     = "ws://"   + SERVER_HOST + ":" + SERVER_PORT;

    private BlockCRDT localDoc = new BlockCRDT();
    private int siteId = 1;
    private BlockID currentBlockId;
    private String currentDocId = null;

    private JTextPane textPane;
    private JLabel statusLabel;
    private JTextField docIdField;
    private JTextField siteIdField;
    private JButton connectBtn;
    private JButton boldBtn;
    private JButton italicBtn;
    private FileToolbar fileToolbar;
    private JButton shareBtn;
    private JLabel usersLabel;
    private JToggleButton viewerBtn;

    private final Map<Integer, Integer> remoteCursors = new LinkedHashMap<>();
    private static final Color[] USER_COLORS = {
            new Color(200, 50,  50),
            new Color(50,  100, 200),
            new Color(30,  160, 30),
            new Color(200, 130, 0)
    };

    private boolean isUpdating = false;

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

        fileToolbar = new FileToolbar(this, siteId);
        fileToolbar.setToolbarListener(this);

        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        docIdField  = new JTextField("myDoc", 10);
        siteIdField = new JTextField("1", 3);
        connectBtn  = new JButton("Connect");

        connectPanel.add(new JLabel("Doc:"));
        connectPanel.add(docIdField);
        connectPanel.add(new JLabel("Site:"));
        connectPanel.add(siteIdField);
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

        shareBtn = new JButton("🔗");
        shareBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        shareBtn.setToolTipText("Share document codes");
        shareBtn.setEnabled(false);
        shareBtn.addActionListener(e -> {
            if (ViewerMode.isViewer()) return;
            if (currentDocId != null) {
                // Pass the HTTP base so ShareManager uses the correct server address
                ShareManager.showShareDialog(this, currentDocId, HTTP_BASE);
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
            connectBtn.setEnabled(true);

            textPane.setEnabled(false);
            boldBtn.setEnabled(false);
            italicBtn.setEnabled(false);
            shareBtn.setEnabled(false);

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
            connectBtn.setEnabled(true);

            textPane.setEnabled(false);
            boldBtn.setEnabled(false);
            italicBtn.setEnabled(false);
            shareBtn.setEnabled(false);

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
        JoinDialog.JoinResult result = JoinDialog.show(this);
        if (result == null) return;

        ViewerMode.setRole(result.role());

        docIdField.setText(result.docId());

        applyRoleToUI();

        handleConnect();
    }

    private void applyRoleToUI() {
        boolean viewer = ViewerMode.isViewer();

        textPane.setEnabled(!viewer);

        boldBtn.setEnabled(!viewer);
        italicBtn.setEnabled(!viewer);

        // Share button: visible and enabled only for editors, but only once connected
        shareBtn.setVisible(!viewer);
        shareBtn.setEnabled(!viewer && NetworkManager.getInstance().isConnected());

        viewerBtn.setSelected(viewer);
        viewerBtn.setText(ViewerMode.label());
        viewerBtn.setEnabled(false);
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

            viewerBtn.setEnabled(false);

            connectBtn.setEnabled(false);
            docIdField.setEnabled(false);
            siteIdField.setEnabled(false);
            textPane.requestFocus();
        }));

        applier.setOnDisconnected(() -> SwingUtilities.invokeLater(() -> {
            NetworkManager.getInstance().getReconnectionHandler().onDisconnected();
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.RED);
            textPane.setEnabled(false);
            boldBtn.setEnabled(false);
            italicBtn.setEnabled(false);
            shareBtn.setEnabled(false);
            viewerBtn.setEnabled(false);
        }));

        String role = ViewerMode.isViewer() ? "VIEWER" : "EDITOR";
        // ── Use WS_BASE (port 8080) instead of hardcoded 8081 ─────────────
        NetworkManager.getInstance().connect(WS_BASE, docId, siteId, applier, role);

        statusLabel.setText("Connecting...");
        statusLabel.setForeground(Color.ORANGE);
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
                        if (e.isControlDown()) { e.consume(); handleUndo(); }
                    }
                    case KeyEvent.VK_Y -> {
                        if (e.isControlDown()) { e.consume(); handleRedo(); }
                    }
                    case KeyEvent.VK_V -> {
                        if (e.isControlDown()) { e.consume(); handlePaste(); }
                    }
                }
            }
        });
    }

    private void setupCaretListener() {
        textPane.addCaretListener(e -> {
            if (!isUpdating && NetworkManager.getInstance().isConnected()) {
                sendCursorUpdate(e.getDot());
            }
        });
    }

    private void handleUndo() {
        if (ViewerMode.isViewer()) return;
        if (!NetworkManager.getInstance().isConnected()) return;
        String inv = undoManager.undo();
        if (inv == null) return;
        try {
            applyLocalOp(new org.json.JSONObject(inv));
            NetworkManager.getInstance().sendRawMessage(inv);
            refreshDisplay();
        } catch (Exception ex) {
            System.err.println("[Undo] " + ex.getMessage());
        }
    }

    private void handleRedo() {
        if (ViewerMode.isViewer()) return;
        if (!NetworkManager.getInstance().isConnected()) return;
        String orig = undoManager.redo();
        if (orig == null) return;
        try {
            applyLocalOp(new org.json.JSONObject(orig));
            NetworkManager.getInstance().sendRawMessage(orig);
            refreshDisplay();
        } catch (Exception ex) {
            System.err.println("[Redo] " + ex.getMessage());
        }
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
                    b.getContent().addChar(new CharNode(cid, pid, j.getString("char").charAt(0)));
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
                    if (localDoc.findBlock(bid) == null) localDoc.addBlock(new Block(bid, par));
                }
                case "delete_block" -> {
                    BlockID bid = new BlockID(j.getJSONObject("blockId").getInt("siteId"), j.getJSONObject("blockId").getInt("counter"));
                    localDoc.deleteBlock(bid);
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

    private Block findBlockStr(String s) {
        String[] p = s.replace("B", "").split("_");
        return localDoc.findBlock(new BlockID(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
    }

    private void handleInsertChar(char ch) {
        if (ViewerMode.isViewer()) return;

        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block      = (Block) blockAndIndex[0];
        int   localIndex = (Integer) blockAndIndex[1];

        CharNode parentNode = getNodeAtVisiblePos(block, localIndex - 1);
        CharID   parentId   = (parentNode != null) ? parentNode.getMyId() : null;

        CharID newCharId  = NetworkManager.getInstance().generateCharID();
        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

        synchronized (localDoc) {
            CharNode newNode = new CharNode(newCharId, parentId, ch);
            block.getContent().addChar(newNode);
        }

        NetworkManager.getInstance().sendInsertChar(blockIdStr, newCharId, parentId, ch);

        String undo = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.deleteChar(blockIdStr, newCharId);
        String orig = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.insertChar(blockIdStr, newCharId, parentId, ch);
        undoManager.record(orig, undo);

        refreshDisplay();
        textPane.setCaretPosition(Math.min(caretPos + 1, getDocumentLength()));
    }

    private void handleBackspace() {
        if (ViewerMode.isViewer()) return;

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

        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

        synchronized (localDoc) { toDelete.markDeleted(); }
        NetworkManager.getInstance().sendDeleteChar(blockIdStr, toDelete.getMyId());

        String del = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.deleteChar(blockIdStr, toDelete.getMyId());
        String ins = com.Team2_CDE_master.ProjectServer.network.OperationSerializer.insertChar(blockIdStr, toDelete.getMyId(), savedParent, savedChar);
        undoManager.record(del, ins);

        refreshDisplay();
        textPane.setCaretPosition(Math.max(0, caretPos - 1));
    }

    private void handleDeleteForward() {
        if (ViewerMode.isViewer()) return;

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

        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());
        synchronized (localDoc) { toDelete.markDeleted(); }
        NetworkManager.getInstance().sendDeleteChar(blockIdStr, toDelete.getMyId());
        refreshDisplay();
        textPane.setCaretPosition(caretPos);
    }

    private void handleEnter() {
        if (ViewerMode.isViewer()) return;

        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block      = (Block) blockAndIndex[0];
        int   localIndex = (Integer) blockAndIndex[1];
        int   blockLen   = block.getContent().getLength();

        BlockID newBlockId = NetworkManager.getInstance().generateBlockID();

        if (localIndex == 0 || localIndex >= blockLen) {
            synchronized (localDoc) {
                Block newBlock = new Block(newBlockId, block.getMyId());
                localDoc.addBlock(newBlock);
                currentBlockId = newBlockId;
            }
            NetworkManager.getInstance().sendInsertBlock(newBlockId, block.getMyId());
        } else {
            synchronized (localDoc) {
                localDoc.splitBlock(block.getMyId(), localIndex, newBlockId);
                currentBlockId = newBlockId;
            }
            NetworkManager.getInstance().sendSplitBlock(block.getMyId(), localIndex, newBlockId);
        }

        refreshDisplay();
        textPane.setCaretPosition(Math.min(caretPos + 1, getDocumentLength()));
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
        textPane.setCaretPosition(Math.max(0, caretPos - 1));
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
        textPane.setCaretPosition(caretPos);
    }

    private void handleFormatting(String formatType) {
        if (ViewerMode.isViewer()) return;

        int start = textPane.getSelectionStart();
        int end   = textPane.getSelectionEnd();
        if (start == end) return;

        int offset = 0;
        for (Block block : localDoc.allBlocks) {
            if (block.checkDeleted()) continue;
            String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

            for (CharNode node : block.getContent().allNodes) {
                if (node.checkDeleted()) continue;
                if (offset >= start && offset < end) {
                    boolean newValue = formatType.equals("bold")
                            ? !node.checkBold() : !node.checkItalic();
                    synchronized (localDoc) {
                        block.getContent().applyFormatting(node.getMyId(), formatType, newValue);
                    }
                    NetworkManager.getInstance().sendFormatting(blockIdStr, node.getMyId(), formatType, newValue);
                }
                offset++;
            }
            offset++;
        }
        refreshDisplay();
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
            isUpdating = false;
            int docLen = textPane.getDocument().getLength();
            textPane.setCaretPosition(Math.min(savedCaret, docLen));
        }
    }

    private void drawRemoteCursors(StyledDocument doc) throws BadLocationException {
        for (Map.Entry<Integer, Integer> entry : remoteCursors.entrySet()) {
            int remoteSiteId = entry.getKey();
            int position     = entry.getValue();
            if (remoteSiteId == siteId) continue;

            int docLen = doc.getLength();
            if (position > docLen) position = docLen;
            if (position < 0)     position = 0;

            Color color = USER_COLORS[remoteSiteId % USER_COLORS.length];
            SimpleAttributeSet cursorStyle = new SimpleAttributeSet();
            StyleConstants.setBackground(cursorStyle, color);
            StyleConstants.setForeground(cursorStyle, Color.WHITE);
            StyleConstants.setBold(cursorStyle, true);

            doc.insertString(position, "|" + remoteSiteId, cursorStyle);
        }
    }

    private void sendCursorUpdate(int caretPos) {
        if (!NetworkManager.getInstance().isConnected()) return;
        try {
            NetworkManager.getInstance().sendCursorUpdate(siteId, caretPos);
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