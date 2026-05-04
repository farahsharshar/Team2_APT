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
        if (!disconnected) return;
        long now = System.currentTimeMillis();
        if (now - disconnectTime > BUFFER_WINDOW_MS) {
            buffer.clear();
            return;
        }
        buffer.add(new TimedMessage(json, now));
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
        Deque<String> ops = getBufferedOps();
        buffer.clear();
        disconnected = false;
        for (String json : ops) {
            nm.sendRawMessage(json);
        }
    }

    public boolean isDisconnected() {
        return disconnected;
    }
}
