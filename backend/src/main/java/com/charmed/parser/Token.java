package com.charmed.parser;

/** Immutable token — output of the lexer. Record gives us equals/hashCode/toString for free. */
public record Token(TokenType type, String value, int line, int column) {
}
