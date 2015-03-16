package com.sequenceiq.cloudbreak.core;

public class InfrastructureException extends CloudbreakException {

    public InfrastructureException(String message) {
        super(message);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }

    public InfrastructureException(Throwable cause) {
        super(cause);
    }

    protected InfrastructureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
