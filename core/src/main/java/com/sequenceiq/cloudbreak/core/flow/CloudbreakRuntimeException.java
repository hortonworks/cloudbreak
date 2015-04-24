package com.sequenceiq.cloudbreak.core.flow;

public class CloudbreakRuntimeException extends RuntimeException {
    public CloudbreakRuntimeException() {
    }

    public CloudbreakRuntimeException(String message) {
        super(message);
    }

    public CloudbreakRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakRuntimeException(Throwable cause) {
        super(cause);
    }

    public CloudbreakRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
