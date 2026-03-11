package com.charmed.command;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;

/** Delete text in a range. Undo re-inserts it. */
public class DeleteTextCommand implements EditorCommand {

    private final Document document;
    private final CursorPosition start;
    private final CursorPosition end;
    private String deletedText;

    public DeleteTextCommand(Document document, CursorPosition start, CursorPosition end) {
        this.document = document;
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute() {
        deletedText = document.deleteRange(start, end);
    }

    @Override
    public void undo() {
        document.insertText(start, deletedText);
    }

    @Override
    public String description() {
        if (deletedText == null) return "Delete";
        String preview = deletedText.length() > 20
                ? deletedText.substring(0, 20) + "..." : deletedText;
        return "Delete '" + preview + "'";
    }
}
