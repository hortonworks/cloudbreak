package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class ScalingFailedException extends CloudbreakServiceException {

    public ScalingFailedException(String message) {
        super(message);
    }

    public ScalingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
