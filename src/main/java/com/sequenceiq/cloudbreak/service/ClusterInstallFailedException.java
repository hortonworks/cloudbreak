package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class ClusterInstallFailedException extends InternalServerException {

    public ClusterInstallFailedException(String message) {
        super(message);
    }

}
