package com.sequenceiq.flow.core.exception;

public class FlowNotTriggerableException extends RuntimeException {

    private final boolean skipException;

    public FlowNotTriggerableException(String message) {
        super(message);
        this.skipException = false;
    }

    public FlowNotTriggerableException(String message, boolean skipException) {
        super(message);
        this.skipException = skipException;
    }

    public boolean isSkipException() {
        return skipException;
    }
}
