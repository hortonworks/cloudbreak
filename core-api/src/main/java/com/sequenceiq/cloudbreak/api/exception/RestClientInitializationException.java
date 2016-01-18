package com.sequenceiq.cloudbreak.api.exception;

public class RestClientInitializationException extends RuntimeException {

    public RestClientInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestClientInitializationException(String message) {
        super(message);
    }
}


