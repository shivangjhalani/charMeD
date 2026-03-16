package com.charmed.editor;

import com.charmed.command.InsertTextCommand;
import com.charmed.command.DeleteTextCommand;
import com.charmed.document.CursorPosition;
import com.charmed.document.Document;

/**
 * Insert mode — text input. Characters inserted at cursor position.
 */
public final class InsertMode implements EditorMode {

    @Override
    public HandleResult handleKey(Editor editor, String key) {
        Document doc = editor.getDocument();
        if (doc == null) return HandleResult.none();

        CursorPosition cursor = doc.getCursor();

        return switch (key) {
            case "enter" -> {
                var cmd = new InsertTextCommand(doc, cursor, "\n");
                editor.getHistory().execute(cmd);
                CursorPosition newCursor = new CursorPosition(cursor.line() + 1, 0);
                doc.setCursor(newCursor);
                editor.getDocumentManager().reparse();
                yield HandleResult.edit(newCursor);
            }
            case "backspace" -> {
                if (cursor.column() > 0) {
                    CursorPosition start = new CursorPosition(cursor.line(), cursor.column() - 1);
                    var cmd = new DeleteTextCommand(doc, start, cursor);
                    editor.getHistory().execute(cmd);
                    doc.setCursor(start);
                    editor.getDocumentManager().reparse();
                    yield HandleResult.edit(start);
                } else if (cursor.line() > 0) {
                    // Join with previous line
                    int prevLineLen = doc.getLine(cursor.line() - 1).length();
                    CursorPosition start = new CursorPosition(cursor.line() - 1, prevLineLen);
                    var cmd = new DeleteTextCommand(doc, start, cursor);
                    editor.getHistory().execute(cmd);
                    doc.setCursor(start);
                    editor.getDocumentManager().reparse();
                    yield HandleResult.edit(start);
                }
                yield HandleResult.none();
            }
            default -> {
                // Regular character input
                if (key.length() == 1 || key.equals("tab")) {
                    String text = key.equals("tab") ? "    " : key;
                    var cmd = new InsertTextCommand(doc, cursor, text);
                    editor.getHistory().execute(cmd);
                    CursorPosition newCursor = new CursorPosition(cursor.line(),
                            cursor.column() + text.length());
                    doc.setCursor(newCursor);
                    editor.getDocumentManager().reparse();
                    yield HandleResult.edit(newCursor);
                }
                yield HandleResult.none();
            }
        };
    }

    @Override
    public String modeName() { return "INSERT"; }
}
