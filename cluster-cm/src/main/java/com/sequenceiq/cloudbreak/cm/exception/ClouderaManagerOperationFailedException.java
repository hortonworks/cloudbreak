package com.sequenceiq.cloudbreak.cm.exception;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class ClouderaManagerOperationFailedException extends CloudbreakServiceException {

    public ClouderaManagerOperationFailedException(String message) {
        super(message);
    }

    public ClouderaManagerOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
