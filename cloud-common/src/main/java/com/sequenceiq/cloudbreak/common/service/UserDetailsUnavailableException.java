package com.sequenceiq.cloudbreak.common.service;

public class UserDetailsUnavailableException extends RuntimeException {

    public UserDetailsUnavailableException(String message) {
        super(message);
    }

    public UserDetailsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
