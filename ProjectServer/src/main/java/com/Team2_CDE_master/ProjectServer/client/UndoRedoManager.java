package com.Team2_CDE_master.ProjectServer.client;

import java.util.ArrayDeque;

public class UndoRedoManager {

    private static final int MAX = 10;

    private final ArrayDeque<String[]> undoStack = new ArrayDeque<>();
    private final ArrayDeque<String[]> redoStack = new ArrayDeque<>();

    public void record(String originalJson, String inverseJson) {
        undoStack.push(new String[]{originalJson, inverseJson});
        redoStack.clear();
        while (undoStack.size() > MAX) {
            undoStack.removeLast();
        }
    }

    public String undo() {
        if (undoStack.isEmpty()) return null;
        String[] entry = undoStack.pop();
        redoStack.push(new String[]{entry[1], entry[0]});
        return entry[1];
    }

    public String redo() {
        if (redoStack.isEmpty()) return null;
        String[] entry = redoStack.pop();
        undoStack.push(new String[]{entry[1], entry[0]});
        return entry[1];
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}