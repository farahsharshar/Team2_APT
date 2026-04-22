package com.Team2_CDE_master.ProjectServer.ui;

import com.Team2_CDE_master.ProjectServer.client.OperationApplier;
import com.Team2_CDE_master.ProjectServer.crdt.*;
import com.Team2_CDE_master.ProjectServer.network.NetworkManager;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
public class EditorWindow extends JFrame {

    private final BlockCRDT localDoc = new BlockCRDT();
    private int siteId = 1;
    private BlockID currentBlockId;

    private JTextPane textPane;
    private JLabel statusLabel;
    private JLabel usersLabel;
    private JTextField docIdField;
    private JTextField siteIdField;
    private JButton connectBtn;
    private JButton boldBtn;
    private JButton italicBtn;

    private final Map<Integer, Integer> remoteCursors = new LinkedHashMap<>();
    private static final Color[] USER_COLORS = {
            new Color(200, 50, 50),
            new Color(50, 100, 200),
            new Color(30, 160, 30),
            new Color(200, 130, 0)
    };

    private boolean isUpdating = false;

    public EditorWindow() {
        super("Collaborative Text Editor — Team 2");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 720);
        setLocationRelativeTo(null);
    }


    private void buildUI() {
        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        connectPanel.setBorder(BorderFactory.createTitledBorder("Join Session"));

        connectPanel.add(new JLabel("Doc ID:"));
        docIdField = new JTextField("myDoc", 12);
        connectPanel.add(docIdField);

        connectPanel.add(new JLabel("My Site ID (1–4):"));
        siteIdField = new JTextField("1", 4);
        connectPanel.add(siteIdField);

        connectBtn = new JButton("Connect");
        connectPanel.add(connectBtn);

        statusLabel = new JLabel("  Not connected");
        statusLabel.setForeground(Color.GRAY);
        connectPanel.add(statusLabel);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        boldBtn = new JButton("Bold");
        boldBtn.setFont(boldBtn.getFont().deriveFont(Font.BOLD));
        boldBtn.setEnabled(false);
        boldBtn.setToolTipText("Toggle bold on selected text");

        italicBtn = new JButton("Italic");
        italicBtn.setFont(italicBtn.getFont().deriveFont(Font.ITALIC));
        italicBtn.setEnabled(false);
        italicBtn.setToolTipText("Toggle italic on selected text");

        usersLabel = new JLabel("  Online: (none)");
        usersLabel.setForeground(Color.DARK_GRAY);

        toolbar.add(boldBtn);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(italicBtn);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(usersLabel);

        textPane = new JTextPane();
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textPane.setEnabled(false);
        textPane.setBackground(new Color(252, 252, 252));
        textPane.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Document"));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(connectPanel);
        northPanel.add(toolbar);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.add(new JLabel("Team 2 — Collaborative Plain Text Editor"));

        setLayout(new BorderLayout(5, 5));
        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        connectBtn.addActionListener(e -> handleConnect());
        boldBtn.addActionListener(e -> handleFormatting("bold"));
        italicBtn.addActionListener(e -> handleFormatting("italic"));
        setupKeyListener();
        setupCaretListener();
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

        OperationApplier applier = new OperationApplier(localDoc);

        applier.setOnDocumentChanged(() ->
                SwingUtilities.invokeLater(this::refreshDisplay));

        applier.setOnCursorUpdate((remoteSiteId, position) ->
                SwingUtilities.invokeLater(() -> {
                    remoteCursors.put(remoteSiteId, position);
                    updateUsersLabel();
                }));

        applier.setOnConnected(() -> SwingUtilities.invokeLater(() -> {
            currentBlockId = new BlockID(siteId, 1);
            if (localDoc.findBlock(currentBlockId) == null) {
                Block firstBlock = new Block(currentBlockId, null);
                synchronized (localDoc) { localDoc.addBlock(firstBlock); }
                NetworkManager.getInstance().sendInsertBlock(currentBlockId, null);
            }
            statusLabel.setText("  Connected — " + docId + "  (site " + siteId + ")");
            statusLabel.setForeground(new Color(0, 130, 0));
            textPane.setEnabled(true);
            boldBtn.setEnabled(true);
            italicBtn.setEnabled(true);
            connectBtn.setEnabled(false);
            docIdField.setEnabled(false);
            siteIdField.setEnabled(false);
            textPane.requestFocus();
        }));

        applier.setOnDisconnected(() -> SwingUtilities.invokeLater(() -> {
            statusLabel.setText("  Disconnected");
            statusLabel.setForeground(Color.RED);
            textPane.setEnabled(false);
            boldBtn.setEnabled(false);
            italicBtn.setEnabled(false);
        }));

        NetworkManager.getInstance().connect("ws://localhost:8081", docId, siteId, applier);
        statusLabel.setText("  Connecting...");
        statusLabel.setForeground(Color.ORANGE);
    }


    private void handleInsertChar(char ch) {
        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block = (Block) blockAndIndex[0];
        int localIndex = (Integer) blockAndIndex[1];

        CharNode parentNode = getNodeAtVisiblePos(block, localIndex - 1);
        CharID parentId = (parentNode != null) ? parentNode.getMyId() : null;

        CharID newCharId = NetworkManager.getInstance().generateCharID();
        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

        synchronized (localDoc) {
            CharNode newNode = new CharNode(newCharId, parentId, ch);
            block.getContent().addChar(newNode);
        }

        NetworkManager.getInstance().sendInsertChar(blockIdStr, newCharId, parentId, ch);

        refreshDisplay();
        textPane.setCaretPosition(Math.min(caretPos + 1, getDocumentLength()));
    }


    private void handleBackspace() {
        int caretPos = textPane.getCaretPosition();
        if (caretPos == 0) return;

        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block = (Block) blockAndIndex[0];
        int localIndex = (Integer) blockAndIndex[1];

        if (localIndex == 0) {
            handleMergeWithPrevious(block);
            return;
        }

        CharNode toDelete = getNodeAtVisiblePos(block, localIndex - 1);
        if (toDelete == null) return;

        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

        synchronized (localDoc) {
            toDelete.markDeleted();
        }

        NetworkManager.getInstance().sendDeleteChar(blockIdStr, toDelete.getMyId());
        refreshDisplay();
        textPane.setCaretPosition(Math.max(0, caretPos - 1));
    }


    private void handleDeleteForward() {
        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block = (Block) blockAndIndex[0];
        int localIndex = (Integer) blockAndIndex[1];
        int blockLen = block.getContent().getLength();

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
        int caretPos = textPane.getCaretPosition();
        Object[] blockAndIndex = findBlockAtCaret(caretPos);
        if (blockAndIndex == null) return;

        Block block = (Block) blockAndIndex[0];
        int localIndex = (Integer) blockAndIndex[1];
        int blockLen = block.getContent().getLength();

        BlockID newBlockId = NetworkManager.getInstance().generateBlockID();
        String blockIdStr = NetworkManager.getInstance().blockIdToString(block.getMyId());

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
                    boolean newValue = formatType.equals("bold") ? !node.checkBold() : !node.checkItalic();
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
                    StyleConstants.setFontFamily(style, "Monospaced");
                    StyleConstants.setFontSize(style, 14);
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
            if (remoteSiteId == siteId) continue;  // skip ourselves

            int docLen = doc.getLength();
            if (position > docLen) position = docLen;
            if (position < 0) position = 0;

            Color color = USER_COLORS[remoteSiteId % USER_COLORS.length];
            SimpleAttributeSet cursorStyle = new SimpleAttributeSet();
            StyleConstants.setBackground(cursorStyle, color);
            StyleConstants.setForeground(cursorStyle, Color.WHITE);
            StyleConstants.setBold(cursorStyle, true);

            String marker = "|" + remoteSiteId;
            doc.insertString(position, marker, cursorStyle);
        }
    }

    private void sendCursorUpdate(int caretPos) {
        if (!NetworkManager.getInstance().isConnected()) return;
        try {
            JSONObject json = new JSONObject();
            json.put("type", "cursor_update");
            json.put("siteId", siteId);
            json.put("position", caretPos);
            NetworkManager.getInstance().sendCursorUpdate(siteId, caretPos);
        } catch (Exception e) {
            System.err.println("[EditorWindow] Cursor update failed: " + e.getMessage());
        }
    }

    private void updateUsersLabel() {
        if (remoteCursors.isEmpty()) {
            usersLabel.setText("  Online: only you");
            return;
        }
        StringBuilder sb = new StringBuilder("  Online: you");
        for (Map.Entry<Integer, Integer> entry : remoteCursors.entrySet()) {
            int id = entry.getKey();
            if (id == siteId) continue;
            Color c = USER_COLORS[id % USER_COLORS.length];
            sb.append(" | ")
                    .append("<font color='#")
                    .append(String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()))
                    .append("'>User ").append(id).append("</font>");
        }
        usersLabel.setText("<html>" + sb + "</html>");
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