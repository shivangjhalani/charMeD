package com.charmed.command;

/** Command pattern interface — every mutation has execute/undo/description. */
public interface EditorCommand {
    void execute();
    void undo();
    String description();
}
