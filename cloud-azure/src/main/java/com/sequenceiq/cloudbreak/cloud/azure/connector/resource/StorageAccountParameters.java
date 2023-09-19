package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.Map;

import com.azure.resourcemanager.storage.models.StorageAccountSkuType;

public class StorageAccountParameters {

    private final String resourceGroupName;

    private final String storageAccountName;

    private final String storageLocation;

    private final StorageAccountSkuType storageAccountSkuType;

    private final Map<String, String> tags;

    public StorageAccountParameters(String resourceGroupName, String storageAccountName, String storageLocation, StorageAccountSkuType storageAccountSkuType,
            Map<String, String> tags) {
        this.resourceGroupName = resourceGroupName;
        this.storageAccountName = storageAccountName;
        this.storageLocation = storageLocation;
        this.storageAccountSkuType = storageAccountSkuType;
        this.tags = tags;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getStorageAccountName() {
        return storageAccountName;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public StorageAccountSkuType getStorageAccountSkuType() {
        return storageAccountSkuType;
    }

    public Map<String, String> getTags() {
        return tags;
    }
}
