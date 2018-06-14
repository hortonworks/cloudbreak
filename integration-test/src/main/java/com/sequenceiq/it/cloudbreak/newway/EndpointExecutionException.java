package com.sequenceiq.it.cloudbreak.newway;

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
