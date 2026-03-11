package com.charmed.document;

import com.charmed.event.DocumentChangedEvent;
import com.charmed.event.EventBus;
import com.charmed.exception.DocumentIOException;
import com.charmed.parser.MarkdownParser;
import com.charmed.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Facade for document operations. Manages the active document,
 * coordinates parsing, and publishes events through the EventBus.
 */
public class DocumentManager {

    private final Parser parser;
    private final EventBus eventBus;
    private Document activeDocument;

    public DocumentManager(EventBus eventBus) {
        this(new MarkdownParser(), eventBus);
    }

    public DocumentManager(Parser parser, EventBus eventBus) {
        this.parser = parser;
        this.eventBus = eventBus;
    }

    /** Open a file, parse it, and set as active document. */
    public Document open(Path path) {
        if (!Files.exists(path)) {
            throw DocumentIOException.fileNotFound(path.toString());
        }
        try {
            String content = Files.readString(path);
            activeDocument = new Document.Builder()
                    .content(content)
                    .filePath(path)
                    .parser(parser)
                    .build();
            return activeDocument;
        } catch (IOException e) {
            throw DocumentIOException.readError(path.toString(), e);
        }
    }

    /** Create a new empty document. */
    public Document newDocument() {
        activeDocument = new Document.Builder()
                .content("")
                .parser(parser)
                .build();
        return activeDocument;
    }

    /** Save the active document to disk. */
    public long save(Path path) {
        if (activeDocument == null) {
            throw new IllegalStateException("No active document");
        }
        Path savePath = path != null ? path : activeDocument.getFilePath();
        if (savePath == null) {
            throw DocumentIOException.noPath();
        }
        try {
            String content = activeDocument.getContent();
            byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(savePath, bytes);
            activeDocument.setFilePath(savePath);
            activeDocument.setDirty(false);
            return bytes.length;
        } catch (IOException e) {
            throw DocumentIOException.writeError(savePath.toString(), e);
        }
    }

    /** Re-parse the active document's content into a fresh AST. */
    public void reparse() {
        if (activeDocument != null) {
            String oldContent = activeDocument.getContent();
            activeDocument.reparse(parser);
            eventBus.publish(new DocumentChangedEvent(oldContent, activeDocument.getContent()));
        }
    }

    public Document getActiveDocument() { return activeDocument; }

    public Parser getParser() { return parser; }
}
