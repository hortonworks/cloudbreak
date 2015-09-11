package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasbintegrated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigException;

@Component
public class StorageAccountStatusCheckerTask implements StatusCheckerTask<StorageAccountCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageAccountStatusCheckerTask.class);

    @Override
    public boolean checkStatus(StorageAccountCheckerContext ctx) {
        AzureRMClient azureClient = new AzureRMClient(ctx.getTenantId(), ctx.getAppId(), ctx.getAppPassword(), ctx.getSubscriptionId());
        try {
            String storageStatus = azureClient.getStorageStatus(ctx.getResourceGroupName(), ctx.getStorageAccountName());
            if ("Succeeded".equals(storageStatus)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.info("Exception occurred while getting status of storage account: ", e);
            return false;
        }
        return false;
    }

    @Override
    public void handleTimeout(StorageAccountCheckerContext storageAccountCheckerContext) {
        throw new FileSystemConfigException("Operation timed out while creating Azure storage account for the WASB filesystem.");
    }

    @Override
    public String successMessage(StorageAccountCheckerContext storageAccountCheckerContext) {
        return "Storage account for the WASB filesystem created successfully.";
    }

    @Override
    public boolean exitPolling(StorageAccountCheckerContext storageAccountCheckerContext) {
        return false;
    }

    @Override
    public boolean handleException(Exception e) {
        throw new FileSystemConfigException("Exception occurred while creating Azure storage account for the WASB filesystem.", e);
    }
}
