package com.charmed.document;

/** Immutable cursor position. Record gives equals/hashCode/toString for free. */
public record CursorPosition(int line, int column) {

    public static final CursorPosition ORIGIN = new CursorPosition(0, 0);

    public CursorPosition {
        if (line < 0) throw new IllegalArgumentException("line must be >= 0");
        if (column < 0) throw new IllegalArgumentException("column must be >= 0");
    }
}
