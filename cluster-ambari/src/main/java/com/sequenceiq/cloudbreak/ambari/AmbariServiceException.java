package com.sequenceiq.cloudbreak.ambari;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class AmbariServiceException extends CloudbreakServiceException {

    public AmbariServiceException(String message) {
        super(message);
    }

    public AmbariServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
