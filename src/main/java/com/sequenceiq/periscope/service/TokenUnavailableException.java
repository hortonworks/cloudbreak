package com.sequenceiq.periscope.service;

public class TokenUnavailableException extends Exception {

    public TokenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenUnavailableException(String message) {
        super(message);
    }
}

