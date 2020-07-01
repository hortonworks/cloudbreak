package com.sequenceiq.cloudbreak.cloud.azure.task.storageaccount;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

public class StorageAccountCheckerContext {

    private final AzureClient azureClient;

    private final String resourceGroupName;

    private final String storageAccountName;

    public StorageAccountCheckerContext(AzureClient azureClient, String resourceGroupName, String storageAccountName) {
        this.azureClient = azureClient;
        this.resourceGroupName = resourceGroupName;
        this.storageAccountName = storageAccountName;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getStorageAccountName() {
        return storageAccountName;
    }
}
