package com.sequenceiq.remoteenvironment;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class RemoteEnvironmentException extends CloudbreakServiceException {
    public RemoteEnvironmentException(String message) {
        super(message);
    }

    public RemoteEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
