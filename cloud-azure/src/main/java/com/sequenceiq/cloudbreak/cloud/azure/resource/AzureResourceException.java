package com.sequenceiq.cloudbreak.cloud.azure.resource;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public class AzureResourceException extends CloudConnectorException {
    public AzureResourceException(Throwable cause) {
        super(cause);
    }

    public AzureResourceException(String message) {
        super(message);
    }

    public AzureResourceException(String message, ResourceType resourceType, String name) {
        super(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name(), name));
    }

    public AzureResourceException(String message, ResourceType resourceType, String name, Throwable cause) {
        this(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name(), name), cause);
    }

    public AzureResourceException(String message, ResourceType resourceType, String name, Long stackId, String operation) {
        super(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, operation: %s ]", message, resourceType.name(), name, stackId, operation));
    }

    public AzureResourceException(String message, ResourceType resourceType, String name, Long stackId, String operation, Throwable cause) {
        this(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, operation: %s ]", message, resourceType.name(), name, stackId, operation),
                cause);
    }

    public AzureResourceException(String message, Throwable cause) {
        super(message + "\n [ Cause message: " + cause.getMessage() + " ]\n", cause);
    }
}
