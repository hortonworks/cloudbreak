package com.sequenceiq.stackpatcher.job;

public class UnknownStackPatchTypeException extends Exception {

    public UnknownStackPatchTypeException(String message) {
        super(message);
    }

    public UnknownStackPatchTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
