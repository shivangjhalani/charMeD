package com.charmed.document;

/** Immutable selection range defined by start and end cursor positions. */
public record SelectionRange(CursorPosition start, CursorPosition end) {

    public SelectionRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end must not be null");
        }
    }

    /** True if the selection is empty (start == end). */
    public boolean isEmpty() {
        return start.equals(end);
    }
}
