package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureClientCachedOperations;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureStorageAccountNameExtractor;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureIDBrokerObjectStorageValidator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.AzureParameters;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Service
public class AzureObjectStorageConnector implements ObjectStorageConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureObjectStorageConnector.class);

    private static final int ACCESS_DENIED_ERROR_CODE = 403;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureIDBrokerObjectStorageValidator azureIDBrokerObjectStorageValidator;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    @Inject
    private AzureClientCachedOperations azureClientCachedOperations;

    @Inject
    private AzureStorageAccountNameExtractor azureStorageAccountNameExtractor;

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        AzureClient client = azureClientService.getClient(request.getCredential());
        String accountId = request.getCredential().getAccountId();
        Optional<StorageAccount> storageAccount = azureClientCachedOperations.getStorageAccount(
                client,
                accountId,
                azureStorageAccountNameExtractor.extractStorageAccountNameIfNecessary(request.getObjectStoragePath()),
                azureUtils.getSupportedAzureStorageKinds());
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
        String accountId = Crn.safeFromString(request.getCredential().getId()).getAccountId();
        if (!entitlementService.azureCloudStorageValidationEnabled(accountId)) {
            LOGGER.info("Azure Cloud storage validation entitlement is missing, not validating cloudStorageRequest: {}",
                    AnonymizerUtil.anonymize(JsonUtil.writeValueAsStringSilent(request)));
            return ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.OK).build();
        }
        AzureClient client = azureClientService.getClient(request.getCredential());
        SpiFileSystem spiFileSystem = request.getSpiFileSystem();
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        resultBuilder.prefix("Cloud Storage validation failed");
        try {
            ValidationResult validationResult = azureIDBrokerObjectStorageValidator.validateObjectStorage(
                    client, accountId, spiFileSystem, request, getSingleResourceGroupName(request), resultBuilder);
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
        } catch (ManagementException e) {
            if (azureExceptionHandler.isForbidden(e)) {
                LOGGER.error("Object storage validation failed on Azure due to authorization failure: {}", e.getMessage());
                throw new ForbiddenException("Object storage validation failed on Azure due to authorization failure: ", e);
            }
            throw azureUtils.convertToCloudConnectorException(e, "Object storage validation");
        }
    }

    private String getSingleResourceGroupName(ObjectStorageValidateRequest request) {
        return Optional.ofNullable(request.getAzure())
                .map(AzureParameters::getSingleResourceGroupName)
                .orElse(null);
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
