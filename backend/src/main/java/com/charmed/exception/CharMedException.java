package com.charmed.exception;

/**
 * Base exception for all charMeD domain errors.
 * Maps errorCode to JSON-RPC error codes for protocol translation.
 */
public abstract class CharMedException extends RuntimeException {

    private final String errorCode;

    protected CharMedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected CharMedException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() { return errorCode; }
}
