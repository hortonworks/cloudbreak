package com.sequenceiq.it.cloudbreak.exception;

public class EndpointExecutionException extends RuntimeException {

    public EndpointExecutionException(String message) {
        super(message);
    }

    public EndpointExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public EndpointExecutionException(Throwable cause) {
        super(cause);
    }
}
