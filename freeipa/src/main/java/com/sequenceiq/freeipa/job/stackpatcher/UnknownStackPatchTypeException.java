package com.sequenceiq.freeipa.job.stackpatcher;

public class UnknownStackPatchTypeException extends Exception {

    public UnknownStackPatchTypeException(String message) {
        super(message);
    }

    public UnknownStackPatchTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
