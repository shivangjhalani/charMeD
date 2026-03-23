package com.charmed.editor;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;
import com.charmed.document.DocumentManager;
import com.charmed.event.CursorMovedEvent;
import com.charmed.event.EventBus;

/**
 * Editor context — holds current mode, delegates key handling to it.
 * Manages Document and EventBus. Provides direct text mutation methods.
 */
public class Editor {

    private EditorMode currentMode;
    private final DocumentManager documentManager;
    private final EventBus eventBus;

    public Editor(DocumentManager documentManager, EventBus eventBus) {
        this.documentManager = documentManager;
        this.eventBus = eventBus;
        this.currentMode = new InsertMode();
    }

    /** Delegate key handling to the current mode. */
    public HandleResult processKey(String key) {
        return currentMode.handleKey(this, key);
    }

    /** Transition to a new editor mode. (Currently locked to InsertMode) */
    public void transitionTo(EditorMode newMode) {
        // No-op: Mode is now locked to InsertMode
    }

    /** Insert text at the given position and reparse the document. */
    public void insertText(CursorPosition position, String text) {
        Document doc = getDocument();
        doc.insertText(position, text);
        documentManager.reparse();
    }

    /** Delete text in the given range and reparse the document. */
    public String deleteText(CursorPosition start, CursorPosition end) {
        Document doc = getDocument();
        String deleted = doc.deleteRange(start, end);
        documentManager.reparse();
        return deleted;
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

    public EventBus getEventBus() { return eventBus; }
}
