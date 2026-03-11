package com.charmed.event;

/** Published when document content changes. */
public class DocumentChangedEvent extends Event {

    private final String oldContent;
    private final String newContent;

    public DocumentChangedEvent(String oldContent, String newContent) {
        super();
        this.oldContent = oldContent;
        this.newContent = newContent;
    }

    public String oldContent() { return oldContent; }
    public String newContent() { return newContent; }
}
