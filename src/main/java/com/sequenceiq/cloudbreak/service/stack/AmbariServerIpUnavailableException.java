package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class AmbariServerIpUnavailableException extends InternalServerException {

    public AmbariServerIpUnavailableException(String message) {
        super(message);
    }

}
