package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.StorageAccount;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureIDBrokerObjectStorageValidator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Service
public class AzureObjectStorageConnector implements ObjectStorageConnector {

    private static final int ACCESS_DENIED_ERROR_CODE = 403;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureIDBrokerObjectStorageValidator azureIDBrokerObjectStorageValidator;

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
    public ObjectStorageValidateResponse validateObjectStorage(ObjectStorageValidateRequest request) {
        AzureClient client = azureClientService.getClient(request.getCredential());
        SpiFileSystem spiFileSystem = request.getSpiFileSystem();
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = azureIDBrokerObjectStorageValidator.validateObjectStorage(
                client, spiFileSystem, resultBuilder);
        ObjectStorageValidateResponse response;
        if (validationResult.hasError()) {
            response = ObjectStorageValidateResponse.builder()
                    .withStatus(ResponseStatus.ERROR)
                    .withError(validationResult.getFormattedErrors())
                    .build();
        } else {
            response = ObjectStorageValidateResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .build();
        }
        return response;
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
