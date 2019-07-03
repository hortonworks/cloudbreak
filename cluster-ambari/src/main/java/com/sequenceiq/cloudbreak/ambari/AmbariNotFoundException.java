package com.sequenceiq.cloudbreak.ambari;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class AmbariNotFoundException extends CloudbreakServiceException {

    public AmbariNotFoundException(String message) {
        super(message);
    }

    public AmbariNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
