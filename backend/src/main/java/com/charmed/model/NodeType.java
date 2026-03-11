package com.charmed.model;

/**
 * Node type discriminator for the markdown AST.
 * Enhanced enum with display name and container flag.
 */
public enum NodeType {
    DOCUMENT("Document", true),
    HEADING("Heading", true),
    PARAGRAPH("Paragraph", true),
    CODE_BLOCK("Code Block", false),
    INLINE_CODE("Inline Code", false),
    LIST("List", true),
    LIST_ITEM("List Item", true),
    BLOCKQUOTE("Blockquote", true),
    BOLD("Bold", true),
    ITALIC("Italic", true),
    LINK("Link", false),
    HORIZONTAL_RULE("Horizontal Rule", false),
    TEXT("Text", false);

    private final String displayName;
    private final boolean isContainer;

    NodeType(String displayName, boolean isContainer) {
        this.displayName = displayName;
        this.isContainer = isContainer;
    }

    public String displayName() { return displayName; }
    public boolean isContainer() { return isContainer; }
}
