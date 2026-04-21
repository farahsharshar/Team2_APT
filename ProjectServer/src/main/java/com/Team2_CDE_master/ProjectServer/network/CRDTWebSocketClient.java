package com.Team2_CDE_master.ProjectServer.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;


public class CRDTWebSocketClient extends WebSocketClient {

    private final OperationListener listener;

    // "ws://localhost:8080"
    // "myDoc123"
    // ws://localhost:8080/document/myDoc123
    public CRDTWebSocketClient(String serverUrl, String docId, OperationListener listener)
            throws URISyntaxException {
        super(new URI(serverUrl + "/document/" + docId));
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("[WebSocket] Connected to server");
        if (listener != null) {
            listener.onConnected();
        }
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WebSocket] Received: " + message);
        if (listener != null) {
            listener.onOperationReceived(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WebSocket] Disconnected. Reason: " + reason +
                " | Remote: " + remote + " | Code: " + code);
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[WebSocket] Error: " + ex.getMessage());
        if (listener != null) {
            listener.onError(ex.getMessage());
        }
    }

    public void sendOperation(String jsonPayload) {
        if (isOpen()) {
            send(jsonPayload);
            System.out.println("[WebSocket] Sent: " + jsonPayload);
        } else {
            System.err.println("[WebSocket] Cannot send — not connected yet");
        }
    }
}
