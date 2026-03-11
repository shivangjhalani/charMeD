package com.charmed.editor;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;

/**
 * Normal mode — navigation, mode switching, undo/redo triggers.
 * h/j/k/l move cursor, i → Insert, : → Command, u → undo, Ctrl+r → redo.
 */
public final class NormalMode implements EditorMode {

    @Override
    public HandleResult handleKey(Editor editor, String key) {
        Document doc = editor.getDocument();
        if (doc == null) {
            return switch (key) {
                case "q" -> HandleResult.quit();
                case ":" -> {
                    editor.transitionTo(new CommandMode());
                    yield HandleResult.modeChange("command");
                }
                default -> HandleResult.none();
            };
        }

        CursorPosition cursor = doc.getCursor();

        return switch (key) {
            // Mode transitions
            case "i" -> {
                editor.transitionTo(new InsertMode());
                yield HandleResult.modeChange("insert");
            }
            case ":" -> {
                editor.transitionTo(new CommandMode());
                yield HandleResult.modeChange("command");
            }

            // Navigation
            case "h", "left" -> moveCursor(editor, doc, cursor, "left", 1);
            case "j", "down" -> moveCursor(editor, doc, cursor, "down", 1);
            case "k", "up" -> moveCursor(editor, doc, cursor, "up", 1);
            case "l", "right" -> moveCursor(editor, doc, cursor, "right", 1);
            case "0" -> moveCursor(editor, doc, cursor, "lineStart", 1);
            case "$" -> moveCursor(editor, doc, cursor, "lineEnd", 1);
            case "g" -> moveCursor(editor, doc, cursor, "documentStart", 1);
            case "G" -> moveCursor(editor, doc, cursor, "documentEnd", 1);

            // Undo/redo
            case "u" -> {
                var undone = editor.getHistory().undo();
                if (undone.isPresent()) {
                    editor.getDocumentManager().reparse();
                    yield HandleResult.edit(doc.getCursor());
                }
                yield HandleResult.none();
            }
            case "ctrl+r" -> {
                var redone = editor.getHistory().redo();
                if (redone.isPresent()) {
                    editor.getDocumentManager().reparse();
                    yield HandleResult.edit(doc.getCursor());
                }
                yield HandleResult.none();
            }

            // Quit
            case "q" -> HandleResult.quit();

            default -> HandleResult.none();
        };
    }

    private HandleResult moveCursor(Editor editor, Document doc, CursorPosition cursor,
                                     String direction, int count) {
        CursorPosition newPos = calculateMove(doc, cursor, direction, count);
        doc.setCursor(newPos);
        editor.publishCursorMoved(cursor, newPos);
        return HandleResult.edit(newPos);
    }

    private CursorPosition calculateMove(Document doc, CursorPosition pos,
                                          String direction, int count) {
        int line = pos.line();
        int col = pos.column();

        return switch (direction) {
            case "up" -> new CursorPosition(Math.max(0, line - count), col);
            case "down" -> new CursorPosition(Math.min(doc.lineCount() - 1, line + count), col);
            case "left" -> new CursorPosition(line, Math.max(0, col - count));
            case "right" -> {
                int maxCol = doc.getLine(line).length();
                yield new CursorPosition(line, Math.min(maxCol, col + count));
            }
            case "lineStart" -> new CursorPosition(line, 0);
            case "lineEnd" -> new CursorPosition(line, doc.getLine(line).length());
            case "documentStart" -> CursorPosition.ORIGIN;
            case "documentEnd" -> new CursorPosition(doc.lineCount() - 1, 0);
            default -> pos;
        };
    }

    @Override
    public String modeName() { return "NORMAL"; }
}
