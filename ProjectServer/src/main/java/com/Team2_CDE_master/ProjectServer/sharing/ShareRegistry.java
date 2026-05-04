package com.Team2_CDE_master.ProjectServer.sharing;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ShareRegistry {

    private final Map<String, ShareCodes> registry = new ConcurrentHashMap<>();

    private static final SecureRandom RNG = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LEN = 8;

    public ShareCodes getOrCreate(String docId) {
        return registry.computeIfAbsent(docId, id ->
                new ShareCodes(generate(), generate()));
    }

    public CodeLookup lookup(String code) {
        for (Map.Entry<String, ShareCodes> e : registry.entrySet()) {
            ShareCodes sc = e.getValue();
            if (sc.editorCode().equals(code))  return new CodeLookup(e.getKey(), Role.EDITOR);
            if (sc.viewerCode().equals(code))  return new CodeLookup(e.getKey(), Role.VIEWER);
        }
        return null;
    }

    public void seed(String docId, String editorCode, String viewerCode) {
        registry.put(docId, new ShareCodes(editorCode, viewerCode));
    }

    private static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public record ShareCodes(String editorCode, String viewerCode) {}

    public record CodeLookup(String docId, Role role) {}

    public enum Role { EDITOR, VIEWER }
}
