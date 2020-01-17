package com.sequenceiq.cloudbreak.util;

public class PermanentlyFailedException extends RuntimeException {

    public PermanentlyFailedException() {
        super("Permanently failed, we don't retry again");
    }

    public PermanentlyFailedException(String message) {
        super(message);
    }

    public PermanentlyFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermanentlyFailedException(Throwable cause) {
        super(cause);
    }

    public PermanentlyFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
