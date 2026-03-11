package com.charmed.command;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;
import com.charmed.document.SelectionRange;

/** Wrap selection in *...* for italic. Undo removes the markers. */
public class FormatItalicCommand implements EditorCommand {

    private final Document document;
    private final SelectionRange range;

    public FormatItalicCommand(Document document, SelectionRange range) {
        this.document = document;
        this.range = range;
    }

    @Override
    public void execute() {
        document.insertText(range.end(), "*");
        document.insertText(range.start(), "*");
    }

    @Override
    public void undo() {
        document.deleteRange(range.start(),
                new CursorPosition(range.start().line(), range.start().column() + 1));
        document.deleteRange(range.end(),
                new CursorPosition(range.end().line(), range.end().column() + 1));
    }

    @Override
    public String description() { return "Format Italic"; }
}
