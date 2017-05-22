package com.sequenceiq.cloudbreak.common.service.user;

public class UserDetailsUnavailableException extends RuntimeException {

    public UserDetailsUnavailableException(String message) {
        super(message);
    }

    public UserDetailsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
