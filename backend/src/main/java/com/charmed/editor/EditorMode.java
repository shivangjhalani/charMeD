package com.charmed.editor;

/**
 * Sealed interface for editor modes. State pattern — each mode encapsulates
 * different behavior for the same key presses.
 *
 * <p>Sealed with exactly 3 permits enables exhaustive pattern matching switch.</p>
 */
public sealed interface EditorMode permits NormalMode, InsertMode, CommandMode {

    /** Handle a key press in this mode. */
    HandleResult handleKey(Editor editor, String key);

    /** Display name for the status bar. */
    String modeName();
}
