package com.Team2_CDE_master.ProjectServer.network;

import java.util.ArrayDeque;
import java.util.Deque;

public class ReconnectionHandler {

    private static final long BUFFER_WINDOW_MS = 5 * 60 * 1000L;

    private record TimedMessage(String json, long timestamp) {}

    private final ArrayDeque<TimedMessage> buffer = new ArrayDeque<>();
    private boolean disconnected = false;
    private long disconnectTime = 0;

    public void onDisconnected() {
        disconnected = true;
        disconnectTime = System.currentTimeMillis();
    }

    public void bufferOp(String json) {
        // ELHEBEISHY'S PART
        if (disconnected) {
            buffer.clear();
        }
    }

    public boolean canReconnect() {
        return disconnected && (System.currentTimeMillis() - disconnectTime <= BUFFER_WINDOW_MS);
    }

    public Deque<String> getBufferedOps() {
        ArrayDeque<String> result = new ArrayDeque<>();
        for (TimedMessage m : buffer) {
            result.add(m.json());
        }
        return result;
    }

    public void onReconnected(NetworkManager nm) {
        // ELHEBEISHY'S PART
        buffer.clear();
        disconnected = false;
        disconnectTime = 0;
    }

    public boolean isDisconnected() {
        return disconnected;
    }
}
