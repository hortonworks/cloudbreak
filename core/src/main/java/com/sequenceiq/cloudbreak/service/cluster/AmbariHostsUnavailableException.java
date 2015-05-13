package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class AmbariHostsUnavailableException extends CloudbreakServiceException {

    public AmbariHostsUnavailableException(String message) {
        super(message);
    }

}
