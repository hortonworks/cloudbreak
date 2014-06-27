package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class AmbariServerIpUnavailableException extends InternalServerException {

    public AmbariServerIpUnavailableException(String message) {
        super(message);
    }

}
