package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class AzureMetadataSetupCheckerTask extends StackBasedStatusCheckerTask<AzureMetadataSetupCheckerTaskContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMetadataSetupCheckerTask.class);

    @Override
    public boolean checkStatus(AzureMetadataSetupCheckerTaskContext azureMetadataSetupCheckerTaskContext) {
        try {
            azureMetadataSetupCheckerTaskContext.getAzureClient().getVirtualMachine(azureMetadataSetupCheckerTaskContext.getProps());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void handleTimeout(AzureMetadataSetupCheckerTaskContext azureMetadataSetupCheckerTaskContext) {
        String message = String.format("Azure Virtualmachine with name: %s is not available; operation timed out.",
                azureMetadataSetupCheckerTaskContext.getProps().get(AzureStackUtil.NAME));
        LOGGER.error(message);
        throw new AzureResourceException(message);
    }

    @Override
    public String successMessage(AzureMetadataSetupCheckerTaskContext azureMetadataSetupCheckerTaskContext) {
        return String.format("Azure virtual machine '%s' is available on '%s' stack",
                azureMetadataSetupCheckerTaskContext.getProps().get(AzureStackUtil.NAME),
                azureMetadataSetupCheckerTaskContext.getStack().getId());
    }

}
