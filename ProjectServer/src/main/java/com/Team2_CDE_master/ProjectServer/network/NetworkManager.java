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

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Person C — Phase 3: added "role" parameter
    // Appends ?role=EDITOR or ?role=VIEWER to the WebSocket URL so the server
    // can enforce permissions for this session.
    // -------------------------------------------------------------------------
    public void connect(String serverUrl, String docId, int siteId,
                        OperationListener listener, String role) {
        this.siteId    = siteId;
        this.listener  = listener;

        // Sanitise role — default to VIEWER if unrecognised (fail-safe)
        String safeRole = (role != null && role.equalsIgnoreCase("EDITOR")) ? "EDITOR" : "VIEWER";
        String fullUrl  = serverUrl + "?role=" + safeRole;

        try {
            wsClient = new CRDTWebSocketClient(fullUrl, docId, listener);
            wsClient.connect();
            System.out.println("Connecting to " + serverUrl + " as site " + siteId + " [" + safeRole + "]");
        } catch (URISyntaxException e) {
            System.err.println("Bad server URL: " + e.getMessage());
            if (listener != null) listener.onError("Bad server URL: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    public boolean isConnected() {
        return wsClient != null && wsClient.isOpen();
    }

    // -------------------------------------------------------------------------
    // ID generators
    // -------------------------------------------------------------------------

    public CharID generateCharID() {
        return new CharID(siteId, charCounter.incrementAndGet());
    }

    public BlockID generateBlockID() {
        return new BlockID(siteId, blockCounter.incrementAndGet());
    }

    public String blockIdToString(BlockID id) {
        return "B" + id.siteId + "_" + id.counter;
    }

    public int getSiteId() {
        return siteId;
    }

    // -------------------------------------------------------------------------
    // Send helpers
    // -------------------------------------------------------------------------

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
        if (!isConnected()) { System.err.println("Not connected"); return; }
        wsClient.sendOperation(json);
    }

    public void sendCursorUpdate(int siteId, int caretPosition) {
        if (!isConnected()) return;
        String json = "{\"type\":\"cursor_update\",\"siteId\":" + siteId
                + ",\"position\":" + caretPosition + "}";
        wsClient.sendOperation(json);
    }
}