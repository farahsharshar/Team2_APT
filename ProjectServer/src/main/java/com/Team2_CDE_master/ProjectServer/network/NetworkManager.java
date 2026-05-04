package com.Team2_CDE_master.ProjectServer.network;

import com.Team2_CDE_master.ProjectServer.crdt.*;

import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkManager {

    private static NetworkManager instance;

    private CRDTWebSocketClient wsClient;
    private OperationListener listener;
    private int siteId;

    private final AtomicInteger charCounter  = new AtomicInteger(0);
    private final AtomicInteger blockCounter = new AtomicInteger(0);

    private final ReconnectionHandler reconnHandler = new ReconnectionHandler();

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    /**
     * @param serverUrl  e.g. "ws://<host>:8081"  (no trailing slash, no path)
     * @param docId      document id
     * @param siteId     this client's site id
     * @param listener   operation listener
     * @param role       "EDITOR" or "VIEWER"
     *
     * Final WebSocket URI: ws://<host>:8081/document/<docId>?role=<role>
     */
    public void connect(String serverUrl, String docId, int siteId,
                        OperationListener listener, String role) {
        this.siteId   = siteId;
        this.listener = listener;

        String safeRole = (role != null && role.equalsIgnoreCase("EDITOR")) ? "EDITOR" : "VIEWER";

        try {
            wsClient = new CRDTWebSocketClient(serverUrl, docId, safeRole, listener);
            wsClient.connect();
            System.out.println("Connecting to " + serverUrl
                    + "/document/" + docId + "?role=" + safeRole
                    + "  (site " + siteId + ")");
        } catch (URISyntaxException e) {
            System.err.println("Bad server URL: " + e.getMessage());
            if (listener != null) listener.onError("Bad server URL: " + e.getMessage());
        }
    }

    public void disconnect() {
        reconnHandler.onDisconnected();
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    public boolean isConnected() {
        return wsClient != null && wsClient.isOpen();
    }

    public CharID generateCharID() {
        return new CharID(siteId, charCounter.incrementAndGet());
    }

    public BlockID generateBlockID() {
        return new BlockID(siteId, blockCounter.incrementAndGet());
    }

    public String blockIdToString(BlockID id) {
        return "B" + id.siteId + "_" + id.counter;
    }

    public int getSiteId() { return siteId; }

    public ReconnectionHandler getReconnectionHandler() { return reconnHandler; }

    // ELHEBEISHY'S PART
    public void syncCountersFrom(BlockCRDT doc) {
        if (doc == null) return;

        int maxChar = charCounter.get();
        int maxBlock = blockCounter.get();

        synchronized (doc) {
            for (Block block : doc.allBlocks) {
                if (block.getMyId().siteId == siteId) {
                    maxBlock = Math.max(maxBlock, block.getMyId().counter);
                }
                for (CharNode node : block.getContent().allNodes) {
                    if (node.getMyId().siteId == siteId) {
                        maxChar = Math.max(maxChar, node.getMyId().myNum);
                    }
                }
            }
        }

        final int syncedChar = maxChar;
        final int syncedBlock = maxBlock;
        charCounter.updateAndGet(current -> Math.max(current, syncedChar));
        blockCounter.updateAndGet(current -> Math.max(current, syncedBlock));
    }

    public void sendInsertChar(String blockId, CharID charId, CharID parentId, char ch) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.insertChar(blockId, charId, parentId, ch));
    }

    public void sendDeleteChar(String blockId, CharID charId) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.deleteChar(blockId, charId));
    }

    public void sendReplaceChar(String blockId, CharID oldCharId, CharNode newNode) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.replaceChar(blockId, oldCharId, newNode));
    }

    public void sendFormatting(String blockId, CharID charId, String formatType, boolean value) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.formatting(blockId, charId, formatType, value));
    }

    public void sendInsertBlock(BlockID blockId, BlockID parentBlockId) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.insertBlock(blockId, parentBlockId));
    }

    public void sendDeleteBlock(BlockID blockId) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.deleteBlock(blockId));
    }

    public void sendSplitBlock(BlockID targetBlockId, int splitIndex, BlockID newBlockId) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.splitBlock(targetBlockId, splitIndex, newBlockId));
    }

    public void sendMergeBlocks(BlockID firstBlockId, BlockID secondBlockId) {
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(OperationSerializer.mergeBlocks(firstBlockId, secondBlockId));
    }

    public void sendRawMessage(String json) {
        if (!isConnected()) {
            // ELHEBEISHY'S PART
            reconnHandler.bufferOp(json);
            System.err.println("Cannot send: not connected");
            return;
        }
        wsClient.sendOperation(json);
    }

    public void sendCursorUpdate(int siteId, int caretPosition) {
        if (!isConnected()) return;
        String json = "{\"type\":\"cursor_update\",\"siteId\":" + siteId
                + ",\"position\":" + caretPosition + "}";
        wsClient.sendOperation(json);
    }
}
