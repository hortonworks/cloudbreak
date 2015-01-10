package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class AzureMetadataSetupException extends InternalServerException {
    public AzureMetadataSetupException(String message) {
        super(message);
    }
}
