package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class OpenStackOperationTimeoutException extends InternalServerException {
    public OpenStackOperationTimeoutException(String message) {
        super(message);
    }
}
