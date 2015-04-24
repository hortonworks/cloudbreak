package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class AmbariOperationFailedException extends InternalServerException {

    public AmbariOperationFailedException(String message) {
        super(message);
    }

    public AmbariOperationFailedException(String message, Throwable cause) { super(message, cause); }

}
