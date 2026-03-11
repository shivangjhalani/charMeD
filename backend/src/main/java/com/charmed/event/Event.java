package com.charmed.event;

import java.time.Instant;

/** Base event with timestamp. All domain events extend this. */
public abstract class Event {

    private final Instant timestamp;

    protected Event() {
        this.timestamp = Instant.now();
    }

    public Instant timestamp() { return timestamp; }
}
