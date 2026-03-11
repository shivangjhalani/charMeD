package com.charmed.event;

import com.charmed.document.CursorPosition;

/** Published when cursor position changes. */
public class CursorMovedEvent extends Event {

    private final CursorPosition oldPosition;
    private final CursorPosition newPosition;

    public CursorMovedEvent(CursorPosition oldPosition, CursorPosition newPosition) {
        super();
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
    }

    public CursorPosition oldPosition() { return oldPosition; }
    public CursorPosition newPosition() { return newPosition; }
}
