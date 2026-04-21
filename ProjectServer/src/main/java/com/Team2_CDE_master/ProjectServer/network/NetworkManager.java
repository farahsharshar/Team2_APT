package com.Team2_CDE_master.ProjectServer.network;

import com.Team2_CDE_master.ProjectServer.crdt.*;

import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

// NetworkManager the whole project talks to this for networking

public class NetworkManager {

    private static NetworkManager instance;
    private CRDTWebSocketClient wsClient;
    private OperationListener listener;
    private int siteId;
    private final AtomicInteger charCounter = new AtomicInteger(0);
    private final AtomicInteger blockCounter = new AtomicInteger(0);
    private NetworkManager() {}
    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public void connect(String serverUrl, String docId, int siteId, OperationListener listener) {
        this.siteId = siteId;
        this.listener = listener;
        try {
            wsClient = new CRDTWebSocketClient(serverUrl, docId, listener);
            wsClient.connect();  // non-blocking — onOpen fires when ready
            System.out.println("[NetworkManager] Connecting to " + serverUrl + " as site " + siteId);
        } catch (URISyntaxException e) {
            System.err.println("[NetworkManager] Bad server URL: " + e.getMessage());
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

    public void sendInsertChar(String blockId, CharID charId, CharID parentId, char ch) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.insertChar(blockId, charId, parentId, ch);
        wsClient.sendOperation(json);
    }

    public void sendDeleteChar(String blockId, CharID charId) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.deleteChar(blockId, charId);
        wsClient.sendOperation(json);
    }

    public void sendReplaceChar(String blockId, CharID oldCharId, CharNode newNode) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.replaceChar(blockId, oldCharId, newNode);
        wsClient.sendOperation(json);
    }

    public void sendFormatting(String blockId, CharID charId, String formatType, boolean value) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.formatting(blockId, charId, formatType, value);
        wsClient.sendOperation(json);
    }
    public void sendInsertBlock(BlockID blockId, BlockID parentBlockId) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.insertBlock(blockId, parentBlockId);
        wsClient.sendOperation(json);
    }
    public void sendDeleteBlock(BlockID blockId) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.deleteBlock(blockId);
        wsClient.sendOperation(json);
    }
    public void sendSplitBlock(BlockID targetBlockId, int splitIndex, BlockID newBlockId) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.splitBlock(targetBlockId, splitIndex, newBlockId);
        wsClient.sendOperation(json);
    }
    public void sendMergeBlocks(BlockID firstBlockId, BlockID secondBlockId) {
        if (!isConnected()) { System.err.println("[NetworkManager] Not connected"); return; }
        String json = OperationSerializer.mergeBlocks(firstBlockId, secondBlockId);
        wsClient.sendOperation(json);
    }
}
