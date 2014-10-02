package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class UserDetailsUnavailableException extends InternalServerException {

    public UserDetailsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
