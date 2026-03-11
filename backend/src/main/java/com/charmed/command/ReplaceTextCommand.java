package com.charmed.command;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;

/** Replace text in a range with new text. Undo restores original. */
public class ReplaceTextCommand implements EditorCommand {

    private final Document document;
    private final CursorPosition start;
    private final CursorPosition end;
    private final String newText;
    private String oldText;

    public ReplaceTextCommand(Document document, CursorPosition start, CursorPosition end, String newText) {
        this.document = document;
        this.start = start;
        this.end = end;
        this.newText = newText;
    }

    @Override
    public void execute() {
        oldText = document.deleteRange(start, end);
        document.insertText(start, newText);
    }

    @Override
    public void undo() {
        // Calculate end position of replacement text
        String[] lines = newText.split("\n", -1);
        int endLine = start.line() + lines.length - 1;
        int endCol = lines.length == 1
                ? start.column() + newText.length()
                : lines[lines.length - 1].length();
        document.deleteRange(start, new CursorPosition(endLine, endCol));
        document.insertText(start, oldText);
    }

    @Override
    public String description() {
        String preview = newText.length() > 20 ? newText.substring(0, 20) + "..." : newText;
        return "Replace with '" + preview + "'";
    }
}
