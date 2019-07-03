package com.sequenceiq.cloudbreak.ambari;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class AmbariHostsUnavailableException extends CloudbreakServiceException {

    public AmbariHostsUnavailableException(String message) {
        super(message);
    }

}
