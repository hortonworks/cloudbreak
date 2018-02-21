package com.sequenceiq.cloudbreak.core;

import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class ClusterException extends CloudbreakException {

    public ClusterException(String message) {
        super(message);
    }

}
