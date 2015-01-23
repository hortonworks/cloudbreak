package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class HeatStackFailedException extends InternalServerException {
    public HeatStackFailedException(String message) {
        super(message);
    }
}
