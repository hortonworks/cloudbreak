package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class AddInstancesFailedException extends InternalServerException {

    public AddInstancesFailedException(String message) {
        super(message);
    }

    public AddInstancesFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
