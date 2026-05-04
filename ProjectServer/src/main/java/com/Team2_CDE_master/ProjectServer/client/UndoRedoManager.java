package com.Team2_CDE_master.ProjectServer.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UndoRedoManager {

    // ELHEBEISHY'S PART
    private static final int MAX = 1000;

    // ELHEBEISHY'S PART
    private static final class OperationGroup {
        private final List<String> originalOps;
        private final List<String> inverseOps;

        private OperationGroup(List<String> originalOps, List<String> inverseOps) {
            this.originalOps = List.copyOf(originalOps);
            this.inverseOps = List.copyOf(inverseOps);
        }
    }

    // ELHEBEISHY'S PART
    private final ArrayDeque<OperationGroup> undoStack = new ArrayDeque<>();
    private final ArrayDeque<OperationGroup> redoStack = new ArrayDeque<>();

    public void record(String originalJson, String inverseJson) {
        // ELHEBEISHY'S PART
        recordGroup(Collections.singletonList(originalJson), Collections.singletonList(inverseJson));
    }

    // ELHEBEISHY'S PART
    public void recordGroup(List<String> originalJsons, List<String> inverseJsons) {
        if (originalJsons == null || inverseJsons == null || originalJsons.isEmpty() || inverseJsons.isEmpty()) {
            return;
        }
        undoStack.push(new OperationGroup(originalJsons, inverseJsons));
        redoStack.clear();
        while (undoStack.size() > MAX) {
            undoStack.removeLast();
        }
    }

    public String undo() {
        // ELHEBEISHY'S PART
        List<String> ops = undoOperations();
        return ops.isEmpty() ? null : ops.get(0);
    }

    public String redo() {
        // ELHEBEISHY'S PART
        List<String> ops = redoOperations();
        return ops.isEmpty() ? null : ops.get(0);
    }

    // ELHEBEISHY'S PART
    public List<String> undoOperations() {
        if (undoStack.isEmpty()) return Collections.emptyList();
        OperationGroup entry = undoStack.pop();
        redoStack.push(new OperationGroup(entry.inverseOps, entry.originalOps));
        return new ArrayList<>(entry.inverseOps);
    }

    // ELHEBEISHY'S PART
    public List<String> redoOperations() {
        if (redoStack.isEmpty()) return Collections.emptyList();
        OperationGroup entry = redoStack.pop();
        undoStack.push(new OperationGroup(entry.inverseOps, entry.originalOps));
        return new ArrayList<>(entry.inverseOps);
    }

    // ELHEBEISHY'S PART
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}
