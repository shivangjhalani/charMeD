package com.charmed.editor;

import com.charmed.command.CommandHistory;
import com.charmed.command.EditorCommand;
import com.charmed.document.CursorPosition;
import com.charmed.document.Document;
import com.charmed.document.DocumentManager;
import com.charmed.event.CursorMovedEvent;
import com.charmed.event.EventBus;

/**
 * Editor context — holds current mode, delegates key handling to it.
 * State pattern context class. Manages Document, CommandHistory, and EventBus.
 */
public class Editor {

    private EditorMode currentMode;
    private final DocumentManager documentManager;
    private final CommandHistory<EditorCommand> history;
    private final EventBus eventBus;

    public Editor(DocumentManager documentManager, EventBus eventBus) {
        this.documentManager = documentManager;
        this.eventBus = eventBus;
        this.history = new CommandHistory<>();
        this.currentMode = new InsertMode();
    }

    /** Delegate key handling to the current mode. */
    public HandleResult processKey(String key) {
        return currentMode.handleKey(this, key);
    }

    /** Transition to a new editor mode. Publishes ModeChangedEvent. */
    public void transitionTo(EditorMode newMode) {
        // No-op: Mode is now locked to InsertMode
    }

    /** Publish a cursor movement event. */
    public void publishCursorMoved(CursorPosition oldPos, CursorPosition newPos) {
        eventBus.publish(new CursorMovedEvent(oldPos, newPos));
    }

    // --- Accessors ---

    public EditorMode currentMode() { return currentMode; }

    public Document getDocument() {
        return documentManager.getActiveDocument();
    }

    public DocumentManager getDocumentManager() { return documentManager; }

    public CommandHistory<EditorCommand> getHistory() { return history; }

    public EventBus getEventBus() { return eventBus; }
}
