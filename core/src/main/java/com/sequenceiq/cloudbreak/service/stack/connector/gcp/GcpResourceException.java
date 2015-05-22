package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.ResourceType;

public class GcpResourceException extends CloudConnectorException {
    public GcpResourceException(Throwable cause) {
        super(cause);
    }

    public GcpResourceException(String message) {
        super(message);
    }

    public GcpResourceException(String message, ResourceType resourceType, String name) {
        super(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name(), name));
    }

    public GcpResourceException(String message, ResourceType resourceType, String name, Throwable cause) {
        this(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name(), name), cause);
    }

    public GcpResourceException(String message, ResourceType resourceType, String name, Long stackId, String operation) {
        super(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, operation: %s ]", message, resourceType.name(), name, stackId, operation));
    }

    public GcpResourceException(String message, ResourceType resourceType, String name, Long stackId, String operation, Throwable cause) {
        this(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, operation: %s ]", message, resourceType.name(), name, stackId, operation),
                cause);
    }

    public GcpResourceException(String message, Throwable cause) {
        super(message + "\n [ Cause message: " + cause.getMessage() + " ]\n", cause);
    }
}
