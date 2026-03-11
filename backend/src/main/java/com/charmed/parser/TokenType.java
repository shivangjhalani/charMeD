package com.charmed.parser;

/** Token types produced by the markdown tokenizer. */
public enum TokenType {
    HEADING,
    PARAGRAPH,
    CODE_FENCE,
    BOLD_MARKER,
    ITALIC_MARKER,
    LINK_OPEN,
    LINK_CLOSE,
    LINK_URL,
    LIST_BULLET,
    LIST_NUMBER,
    BLOCKQUOTE_MARKER,
    HORIZONTAL_RULE,
    TEXT,
    NEWLINE,
    EOF
}
