package com.sequenceiq.cloudbreak.cloud.azure.validator;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.azure.AzureSetup;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;

@Component
public class AzureFileSystemValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFileSystemValidator.class);

    @Inject
    private AzureSetup azureSetup;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        if (cloudStack.getFileSystem().isEmpty()) {
            return;
        }
        LOGGER.info("Validation azure filesystem");
        try {
            SpiFileSystem spiFileSystem = cloudStack.getFileSystem().get();
            azureSetup.validateFileSystem(ac.getCloudCredential(), spiFileSystem);
        } catch (Exception e) {
            LOGGER.error("Error during the azure file system validation", e);
            throw new CloudConnectorException("Error during the azure file system validation", e);
        }
    }
}
