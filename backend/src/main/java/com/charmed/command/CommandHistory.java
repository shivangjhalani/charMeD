package com.charmed.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Generic undo/redo history. Bounded type parameter ensures type safety.
 * Generics showcase: T extends EditorCommand.
 */
public class CommandHistory<T extends EditorCommand> {

    private final Deque<T> undoStack = new ArrayDeque<>();
    private final Deque<T> redoStack = new ArrayDeque<>();

    /** Execute command and push to undo stack. Clears redo history. */
    public void execute(T command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    /** Undo last command. Returns the undone command or empty. */
    public Optional<T> undo() {
        if (undoStack.isEmpty()) return Optional.empty();
        T command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return Optional.of(command);
    }

    /** Redo last undone command. Returns the redone command or empty. */
    public Optional<T> redo() {
        if (redoStack.isEmpty()) return Optional.empty();
        T command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return Optional.of(command);
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
}
