package com.sequenceiq.periscope.api.exception;

public class TokenUnavailableException extends RuntimeException {

    public TokenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenUnavailableException(String message) {
        super(message);
    }
}


