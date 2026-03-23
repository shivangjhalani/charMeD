package com.charmed.editor;

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
                editor.insertText(cursor, "\n");
                CursorPosition newCursor = new CursorPosition(cursor.line() + 1, 0);
                doc.setCursor(newCursor);
                yield HandleResult.edit(newCursor);
            }
            case "backspace" -> {
                if (cursor.column() > 0) {
                    CursorPosition start = new CursorPosition(cursor.line(), cursor.column() - 1);
                    editor.deleteText(start, cursor);
                    doc.setCursor(start);
                    yield HandleResult.edit(start);
                } else if (cursor.line() > 0) {
                    int prevLineLen = doc.getLine(cursor.line() - 1).length();
                    CursorPosition start = new CursorPosition(cursor.line() - 1, prevLineLen);
                    editor.deleteText(start, cursor);
                    doc.setCursor(start);
                    yield HandleResult.edit(start);
                }
                yield HandleResult.none();
            }
            default -> {
                if (key.length() == 1 || key.equals("tab")) {
                    String text = key.equals("tab") ? "    " : key;
                    editor.insertText(cursor, text);
                    CursorPosition newCursor = new CursorPosition(cursor.line(),
                            cursor.column() + text.length());
                    doc.setCursor(newCursor);
                    yield HandleResult.edit(newCursor);
                }
                yield HandleResult.none();
            }
        };
    }

    @Override
    public String modeName() { return "INSERT"; }
}
