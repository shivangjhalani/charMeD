package com.charmed.exception;

/** Command execution/undo failure. */
public class CommandExecutionException extends CharMedException {

    public CommandExecutionException(String message) {
        super(message, "-32603");
    }

    public CommandExecutionException(String message, Throwable cause) {
        super(message, "-32603", cause);
    }
}
