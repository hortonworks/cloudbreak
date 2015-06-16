package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AzureResourceException extends CloudConnectorException {
    public AzureResourceException(String message) {
        super(message);
    }

    public AzureResourceException(Throwable cause) {
        super(cause);
    }
}
