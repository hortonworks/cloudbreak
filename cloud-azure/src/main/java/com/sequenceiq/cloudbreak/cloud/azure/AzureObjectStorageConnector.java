package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.StorageAccount;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

@Service
public class AzureObjectStorageConnector implements ObjectStorageConnector {

    private static final int ACCESS_DENIED_ERROR_CODE = 403;

    @Inject
    private AzureClientService azureClientService;

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        AzureClient client = azureClientService.getClient(request.getCredential());

        Optional<StorageAccount> storageAccount = client.getStorageAccount(request.getObjectStoragePath(), Kind.STORAGE_V2);
        if (storageAccount.isPresent()) {
            return ObjectStorageMetadataResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .withRegion(storageAccount.get().region().name())
                    .build();
        } else {
            return ObjectStorageMetadataResponse.builder()
                    .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                    .build();
        }

    }

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }
}
