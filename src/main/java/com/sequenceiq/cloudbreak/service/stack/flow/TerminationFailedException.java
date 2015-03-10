package com.sequenceiq.cloudbreak.service.stack.flow;

public class TerminationFailedException extends RuntimeException {

    public TerminationFailedException() { }

    public TerminationFailedException(String message) {
        super(message);
    }

    public TerminationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TerminationFailedException(Throwable cause) {
        super(cause);
    }

    protected TerminationFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
