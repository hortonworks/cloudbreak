package com.sequenceiq.cloudbreak.service.stack.flow;

public class CleanUpException extends RuntimeException {

    public CleanUpException(String message) {
        super(message);
    }

    public CleanUpException(String message, Throwable cause) {
        super(message, cause);
    }

}
