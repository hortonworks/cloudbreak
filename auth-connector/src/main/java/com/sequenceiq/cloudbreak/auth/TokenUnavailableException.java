package com.sequenceiq.cloudbreak.auth;

public class TokenUnavailableException extends RuntimeException {

    public TokenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenUnavailableException(String message) {
        super(message);
    }
}


