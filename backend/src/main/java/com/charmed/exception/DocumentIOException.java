package com.charmed.exception;

/** File I/O failure — read/write/permission errors. */
public class DocumentIOException extends CharMedException {

    public DocumentIOException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DocumentIOException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    public static DocumentIOException fileNotFound(String path) {
        return new DocumentIOException("File not found: " + path, "-32001");
    }

    public static DocumentIOException readError(String path, Throwable cause) {
        return new DocumentIOException("Cannot read file: " + path, "-32002", cause);
    }

    public static DocumentIOException writeError(String path, Throwable cause) {
        return new DocumentIOException("Cannot write file: " + path, "-32004", cause);
    }

    public static DocumentIOException noPath() {
        return new DocumentIOException("No file path specified for save", "-32005");
    }
}
