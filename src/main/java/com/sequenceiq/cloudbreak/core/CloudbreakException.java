package com.sequenceiq.cloudbreak.core;

public class CloudbreakException extends Exception {
    public CloudbreakException() {

    }

    public CloudbreakException(String message) {
        super(message);
    }

    public CloudbreakException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
