package com.sequenceiq.cloudbreak.orcestrator;

public class CloudbreakOrcestratorException extends Exception {

    public CloudbreakOrcestratorException(String message) {
        super(message);
    }

    public CloudbreakOrcestratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakOrcestratorException(Throwable cause) {
        super(cause);
    }

    protected CloudbreakOrcestratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
