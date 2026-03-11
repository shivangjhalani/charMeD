package com.charmed.editor;

import com.charmed.document.CursorPosition;

/**
 * Result of handling a key press in an editor mode.
 * Record for immutable result data.
 */
public record HandleResult(
        Action action,
        String data,
        CursorPosition newCursor
) {
    public enum Action {
        /** No state change needed. */
        NONE,
        /** Mode transition requested. */
        MODE_CHANGE,
        /** Document edit performed. */
        EDIT,
        /** Command-mode command entered. */
        COMMAND,
        /** Quit requested. */
        QUIT
    }

    public static HandleResult none() {
        return new HandleResult(Action.NONE, null, null);
    }

    public static HandleResult modeChange(String mode) {
        return new HandleResult(Action.MODE_CHANGE, mode, null);
    }

    public static HandleResult edit(CursorPosition cursor) {
        return new HandleResult(Action.EDIT, null, cursor);
    }

    public static HandleResult command(String commandText) {
        return new HandleResult(Action.COMMAND, commandText, null);
    }

    public static HandleResult quit() {
        return new HandleResult(Action.QUIT, null, null);
    }
}
