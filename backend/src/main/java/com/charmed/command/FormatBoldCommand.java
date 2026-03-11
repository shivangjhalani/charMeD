package com.charmed.command;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;
import com.charmed.document.SelectionRange;

/** Wrap selection in **...** for bold. Undo removes the markers. */
public class FormatBoldCommand implements EditorCommand {

    private final Document document;
    private final SelectionRange range;

    public FormatBoldCommand(Document document, SelectionRange range) {
        this.document = document;
        this.range = range;
    }

    @Override
    public void execute() {
        // Insert closing ** first (so positions stay valid), then opening **
        document.insertText(range.end(), "**");
        document.insertText(range.start(), "**");
    }

    @Override
    public void undo() {
        // Remove opening ** first, then closing **
        document.deleteRange(range.start(),
                new CursorPosition(range.start().line(), range.start().column() + 2));
        document.deleteRange(range.end(),
                new CursorPosition(range.end().line(), range.end().column() + 2));
    }

    @Override
    public String description() { return "Format Bold"; }
}
