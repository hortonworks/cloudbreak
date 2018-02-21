package com.sequenceiq.cloudbreak.core;

import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class CloudbreakSecuritySetupException extends CloudbreakException {

    public CloudbreakSecuritySetupException(String message) {
        super(message);
    }

    public CloudbreakSecuritySetupException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakSecuritySetupException(Throwable cause) {
        super(cause);
    }
}
