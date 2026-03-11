package com.charmed.command;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;

/** Insert text at a position. Undo removes it. */
public class InsertTextCommand implements EditorCommand {

    private final Document document;
    private final CursorPosition position;
    private final String text;

    public InsertTextCommand(Document document, CursorPosition position, String text) {
        this.document = document;
        this.position = position;
        this.text = text;
    }

    @Override
    public void execute() {
        document.insertText(position, text);
    }

    @Override
    public void undo() {
        // Calculate end position of inserted text
        String[] insertedLines = text.split("\n", -1);
        int endLine = position.line() + insertedLines.length - 1;
        int endCol;
        if (insertedLines.length == 1) {
            endCol = position.column() + text.length();
        } else {
            endCol = insertedLines[insertedLines.length - 1].length();
        }
        document.deleteRange(position, new CursorPosition(endLine, endCol));
    }

    @Override
    public String description() {
        String preview = text.length() > 20 ? text.substring(0, 20) + "..." : text;
        return "Insert '" + preview + "'";
    }
}
