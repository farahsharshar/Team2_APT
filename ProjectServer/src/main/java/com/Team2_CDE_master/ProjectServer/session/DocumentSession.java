package com.Team2_CDE_master.ProjectServer.session;

import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentSession {
    // docId → its live CRDT state (in memory while the server is running)
    private static final ConcurrentHashMap<String, BlockCRDT> documents = new ConcurrentHashMap<>();

    // Returns the session if it exists, or creates a new empty one.
    // Called by the WebSocket handler on every incoming operation.
    public static BlockCRDT getOrCreate(String docId) {
        return documents.computeIfAbsent(docId, id -> new BlockCRDT());
    }

    // Returns the session if it exists, or null if nobody has connected yet.
    // Used by DocumentController to check if there is a live session to save.
    public static BlockCRDT get(String docId) {
        return documents.get(docId);
    }

    // Loads a pre-built BlockCRDT (from the database) into the session map.
    // If a session for this docId already exists it is replaced.
    // Called when a document is loaded from DB before any client connects.
    public static void seed(String docId, BlockCRDT doc) {
        documents.put(docId, doc);
    }

    // Returns all active sessions — used by the auto-save scheduler.
    public static Map<String, BlockCRDT> getAllSessions() {
        return documents;
    }
}