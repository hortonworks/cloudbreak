package com.sequenceiq.cloudbreak.ambari;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class AmbariNotFoundException extends CloudbreakServiceException {

    public AmbariNotFoundException(String message) {
        super(message);
    }

    public AmbariNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
