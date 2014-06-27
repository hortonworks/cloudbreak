package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class ClusterInstallFailedException extends InternalServerException {

    public ClusterInstallFailedException(String message) {
        super(message);
    }

}
