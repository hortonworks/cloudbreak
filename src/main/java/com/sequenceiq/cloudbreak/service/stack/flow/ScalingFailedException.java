package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class ScalingFailedException extends InternalServerException {

    public ScalingFailedException(String message) {
        super(message);
    }

    public ScalingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
