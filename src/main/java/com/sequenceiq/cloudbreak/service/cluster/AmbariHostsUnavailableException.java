package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class AmbariHostsUnavailableException extends InternalServerException {

    public AmbariHostsUnavailableException(String message) {
        super(message);
    }

}
