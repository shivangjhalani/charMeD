package com.charmed.event;

/** Published when editor mode changes (Normal/Insert/Command). */
public class ModeChangedEvent extends Event {

    private final String modeName;

    public ModeChangedEvent(String modeName) {
        super();
        this.modeName = modeName;
    }

    public String modeName() { return modeName; }
}
