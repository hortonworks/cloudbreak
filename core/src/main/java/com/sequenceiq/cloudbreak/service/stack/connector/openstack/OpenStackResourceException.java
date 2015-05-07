package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;

public class OpenStackResourceException extends CloudConnectorException {
    public OpenStackResourceException(String message) {
        super(message);
    }
}
