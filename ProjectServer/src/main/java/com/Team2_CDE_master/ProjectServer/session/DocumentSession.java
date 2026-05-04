package com.Team2_CDE_master.ProjectServer.session;

import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentSession {
    private static final ConcurrentHashMap<String, BlockCRDT> documents = new ConcurrentHashMap<>();

    public static BlockCRDT getOrCreate(String docId) {
        return documents.computeIfAbsent(docId, id -> new BlockCRDT());
    }

    public static BlockCRDT get(String docId) {
        return documents.get(docId);
    }

    public static void seed(String docId, BlockCRDT doc) {
        documents.put(docId, doc);
    }

    public static Map<String, BlockCRDT> getAllSessions() {
        return documents;
    }
}
