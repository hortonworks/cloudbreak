package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;

public class AzureResourceException extends CloudConnectorException {
    public AzureResourceException(String message) {
        super(message);
    }

    public AzureResourceException(Throwable cause) {
        super(cause);
    }

    public AzureResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
