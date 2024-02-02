package com.sequenceiq.cloudbreak.cloud.azure.service;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.storage.models.Kind;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@Component
public class AzureClientCachedOperations {

    @Cacheable(value = AzureClientOperationsCache.AZURE_CLIENT_OPERATIONS_CACHE, key = "{ #accountId,#storageName }")
    public Optional<StorageAccount> getStorageAccount(AzureClient azureClient, String accountId, String storageName, Kind accountKind) {
        return azureClient.getStorageAccount(storageName, accountKind);
    }
}
