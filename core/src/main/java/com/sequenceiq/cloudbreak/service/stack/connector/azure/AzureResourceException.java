package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureResourceException extends CloudConnectorException {
    public AzureResourceException(String message) {
        super(message);
    }

    public AzureResourceException(Throwable cause) {
        super(cause);
    }
}
