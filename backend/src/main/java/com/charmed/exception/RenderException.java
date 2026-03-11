package com.charmed.exception;

/** Rendering failure — export format errors. */
public class RenderException extends CharMedException {

    public RenderException(String message) {
        super(message, "-32007");
    }

    public RenderException(String message, Throwable cause) {
        super(message, "-32007", cause);
    }
}
