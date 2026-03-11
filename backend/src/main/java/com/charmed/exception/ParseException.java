package com.charmed.exception;

/** Markdown parsing failure — unexpected tokens, malformed syntax. */
public class ParseException extends CharMedException {

    private final int line;
    private final int column;

    public ParseException(String message, int line, int column) {
        super(message, "-32003");
        this.line = line;
        this.column = column;
    }

    public ParseException(String message, int line, int column, Throwable cause) {
        super(message, "-32003", cause);
        this.line = line;
        this.column = column;
    }

    public int line() { return line; }
    public int column() { return column; }
}
