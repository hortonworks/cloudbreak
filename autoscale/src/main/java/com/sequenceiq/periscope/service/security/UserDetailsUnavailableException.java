package com.sequenceiq.periscope.service.security;

public class UserDetailsUnavailableException extends RuntimeException {

    public UserDetailsUnavailableException(String message) {
        super(message);
    }

    public UserDetailsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
