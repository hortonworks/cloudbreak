package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.ResourceType;

public class OpenStackResourceException extends CloudConnectorException {
    public OpenStackResourceException(Throwable cause) {
        super(cause);
    }

    public OpenStackResourceException(String message) {
        super(message);
    }

    public OpenStackResourceException(String message, ResourceType resourceType, String name) {
        super(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name(), name));
    }

    public OpenStackResourceException(String message, ResourceType resourceType, String name, Throwable cause) {
        this(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name(), name), cause);
    }

    public OpenStackResourceException(String message, ResourceType resourceType, String name, Long stackId, String errorDescription) {
        super(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, errorDesc: %s ]", message, resourceType.name(), name, stackId,
                errorDescription));
    }

    public OpenStackResourceException(String message, ResourceType resourceType, String name, Long stackId, String errorDescription, Throwable cause) {
        this(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, errorDesc: %s ]", message, resourceType.name(), name, stackId,
                errorDescription), cause);
    }

    public OpenStackResourceException(String message, Throwable cause) {
        super(message + "\n [ Cause message: " + cause.getMessage() + " ]\n", cause);
    }
}
