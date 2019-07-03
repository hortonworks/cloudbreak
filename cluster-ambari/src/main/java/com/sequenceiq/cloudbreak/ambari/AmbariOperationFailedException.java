package com.sequenceiq.cloudbreak.ambari;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class AmbariOperationFailedException extends CloudbreakServiceException {

    public AmbariOperationFailedException(String message) {
        super(message);
    }

    public AmbariOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
