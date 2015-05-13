package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class AmbariOperationFailedException extends CloudbreakServiceException {

    public AmbariOperationFailedException(String message) {
        super(message);
    }

    public AmbariOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
