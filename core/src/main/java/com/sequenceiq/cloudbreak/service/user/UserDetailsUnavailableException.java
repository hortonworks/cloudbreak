package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class UserDetailsUnavailableException extends CloudbreakServiceException {

    public UserDetailsUnavailableException(String message) {
        super(message);
    }

    public UserDetailsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
