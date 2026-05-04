package com.Team2_CDE_master.ProjectServer.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class CRDTWebSocketClient extends WebSocketClient {

    private final OperationListener listener;

    /**
     * @param serverUrl  base WS URL, e.g. "ws://localhost:8080"
     * @param docId      document ID
     * @param role       "EDITOR" or "VIEWER"
     * @param listener   operation listener
     *
     * Final URI: ws://localhost:8080/document/<docId>?role=<role>
     */
    public CRDTWebSocketClient(String serverUrl, String docId, String role, OperationListener listener)
            throws URISyntaxException {
        super(new URI(serverUrl + "/document/" + docId + "?role=" + role));
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server");
        if (listener != null) {
            listener.onConnected();
        }
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        if (listener != null) {
            listener.onOperationReceived(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected. Reason: " + reason
                + ", Remote: " + remote + ", Code: " + code);
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        if (listener != null) {
            listener.onError(ex.getMessage());
        }
    }

    public void sendOperation(String jsonPayload) {
        if (isOpen()) {
            // ELHEBEISHY'S PART
            try {
                send(jsonPayload);
                System.out.println("Sent: " + jsonPayload);
            } catch (Exception ex) {
                System.err.println("Send failed: " + ex.getMessage());
                if (listener != null) {
                    listener.onError(ex.getMessage());
                }
            }
        } else {
            System.err.println("Cannot send: not connected");
        }
    }
}
