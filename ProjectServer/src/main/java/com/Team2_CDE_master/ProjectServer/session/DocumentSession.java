package com.Team2_CDE_master.ProjectServer.session;

import com.Team2_CDE_master.ProjectServer.crdt.BlockCRDT;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentSession {
    // docId → its CRDT state
    private static final ConcurrentHashMap<String, BlockCRDT> documents = new ConcurrentHashMap<>();

    public static BlockCRDT getOrCreate(String docId) {
        return documents.computeIfAbsent(docId, id -> new BlockCRDT());
    }
}