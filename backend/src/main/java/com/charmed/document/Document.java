package com.charmed.document;

import com.charmed.model.MarkdownNode;
import com.charmed.parser.MarkdownParser;
import com.charmed.parser.Parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Document model — holds raw content (as lines), parsed AST, cursor, and state.
 * Builder pattern for step-by-step construction.
 */
public class Document {

    private final List<String> lines;
    private MarkdownNode ast;
    private CursorPosition cursor;
    private SelectionRange selection;
    private Path filePath;
    private boolean dirty;

    private Document(Builder builder) {
        this.lines = new ArrayList<>(builder.lines);
        this.cursor = builder.cursor;
        this.filePath = builder.filePath;
        this.dirty = false;
        this.selection = null;
        // Parse the content into AST
        this.ast = builder.parser.parse(getContent());
    }

    // --- Content access ---

    public String getContent() {
        return String.join("\n", lines);
    }

    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public int lineCount() { return lines.size(); }

    public String getLine(int index) {
        if (index < 0 || index >= lines.size()) return "";
        return lines.get(index);
    }

    // --- Mutation (used by commands) ---

    public void insertText(CursorPosition pos, String text) {
        String[] insertLines = text.split("\n", -1);
        if (pos.line() >= lines.size()) {
            while (lines.size() <= pos.line()) lines.add("");
        }
        String currentLine = lines.get(pos.line());
        int col = Math.min(pos.column(), currentLine.length());

        if (insertLines.length == 1) {
            // Single line insert
            lines.set(pos.line(), currentLine.substring(0, col) + text + currentLine.substring(col));
        } else {
            // Multi-line insert
            String before = currentLine.substring(0, col);
            String after = currentLine.substring(col);
            lines.set(pos.line(), before + insertLines[0]);
            for (int i = 1; i < insertLines.length - 1; i++) {
                lines.add(pos.line() + i, insertLines[i]);
            }
            lines.add(pos.line() + insertLines.length - 1, insertLines[insertLines.length - 1] + after);
        }
        dirty = true;
    }

    public String deleteRange(CursorPosition start, CursorPosition end) {
        if (start.line() == end.line()) {
            String line = lines.get(start.line());
            int startCol = Math.min(start.column(), line.length());
            int endCol = Math.min(end.column(), line.length());
            String deleted = line.substring(startCol, endCol);
            lines.set(start.line(), line.substring(0, startCol) + line.substring(endCol));
            dirty = true;
            return deleted;
        }
        // Multi-line delete
        StringBuilder deleted = new StringBuilder();
        String startLine = lines.get(start.line());
        String endLine = lines.get(end.line());
        deleted.append(startLine.substring(Math.min(start.column(), startLine.length())));

        for (int i = start.line() + 1; i < end.line(); i++) {
            deleted.append("\n").append(lines.get(start.line() + 1));
            lines.remove(start.line() + 1);
        }
        deleted.append("\n").append(endLine, 0, Math.min(end.column(), endLine.length()));

        // Merge remaining
        lines.set(start.line(),
                startLine.substring(0, Math.min(start.column(), startLine.length()))
                + endLine.substring(Math.min(end.column(), endLine.length())));
        lines.remove(start.line() + 1);

        dirty = true;
        return deleted.toString();
    }

    // --- AST ---

    public MarkdownNode getAst() { return ast; }

    public void reparse(Parser parser) {
        this.ast = parser.parse(getContent());
    }

    // --- Cursor & Selection ---

    public CursorPosition getCursor() { return cursor; }
    public void setCursor(CursorPosition cursor) { this.cursor = cursor; }

    public SelectionRange getSelection() { return selection; }
    public void setSelection(SelectionRange selection) { this.selection = selection; }

    // --- File state ---

    public Path getFilePath() { return filePath; }
    public void setFilePath(Path path) { this.filePath = path; }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    // --- Builder ---

    public static class Builder {
        private final List<String> lines = new ArrayList<>();
        private CursorPosition cursor = CursorPosition.ORIGIN;
        private Path filePath = null;
        private Parser parser = new MarkdownParser();

        public Builder content(String rawMarkdown) {
            this.lines.clear();
            if (rawMarkdown == null || rawMarkdown.isEmpty()) {
                this.lines.add("");
            } else {
                Collections.addAll(this.lines, rawMarkdown.split("\n", -1));
            }
            return this;
        }

        public Builder filePath(Path path) {
            this.filePath = path;
            return this;
        }

        public Builder cursor(CursorPosition cursor) {
            this.cursor = cursor;
            return this;
        }

        public Builder parser(Parser parser) {
            this.parser = parser;
            return this;
        }

        public Document build() {
            if (lines.isEmpty()) lines.add("");
            return new Document(this);
        }
    }
}
