package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@Component
public class AzureMetadataSetupCheckerTask implements StatusCheckerTask<AzureMetadataSetupCheckerTaskContext> {

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
        MDCBuilder.buildMdcContext(azureMetadataSetupCheckerTaskContext.getStack());
        String message = String.format("Azure Virtualmachine with name: %s not available in the timeframe.",
                azureMetadataSetupCheckerTaskContext.getProps().get(AzureStackUtil.NAME));
        LOGGER.error(message);
        throw new AzureMetadataSetupException(message);
    }

    @Override
    public String successMessage(AzureMetadataSetupCheckerTaskContext azureMetadataSetupCheckerTaskContext) {
        MDCBuilder.buildMdcContext(azureMetadataSetupCheckerTaskContext.getStack());
        return String.format("Azure virtual machine '%s' is available on '%s' stack",
                azureMetadataSetupCheckerTaskContext.getProps().get(AzureStackUtil.NAME),
                azureMetadataSetupCheckerTaskContext.getStack().getId());
    }
}
