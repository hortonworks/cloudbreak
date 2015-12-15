package com.sequenceiq.cloudbreak.shell.configuration;

public class TokenUnavailableException extends RuntimeException {

    public TokenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenUnavailableException(String message) {
        super(message);
    }
}

