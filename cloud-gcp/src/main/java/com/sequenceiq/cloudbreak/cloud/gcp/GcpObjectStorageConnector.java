package com.sequenceiq.cloudbreak.cloud.gcp;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.storage.Storage;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.validator.GcpServiceAccountObjectStorageValidator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;

@Service
public class GcpObjectStorageConnector implements ObjectStorageConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpObjectStorageConnector.class);

    @Inject
    private GcpServiceAccountObjectStorageValidator gcpServiceAccountObjectStorageValidator;

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        Storage storage = GcpStackUtil.buildStorage(request.getCredential(), request.getCredential().getName());
        try {
            storage.buckets().get(GcpStackUtil.getBucketName(request.getObjectStoragePath())).execute();
            return ObjectStorageMetadataResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .build();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage());
            return ObjectStorageMetadataResponse.builder()
                    .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                    .build();
        }
    }

    @Override
    public ObjectStorageValidateResponse validateObjectStorage(ObjectStorageValidateRequest request) {
        Storage storage = GcpStackUtil.buildStorage(request.getCredential(), request.getCredential().getName());
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();

        for (StorageLocationBase location : request.getCloudStorageRequest().getLocations()) {
            String bucketName = GcpStackUtil.getBucketName(location.getValue());
            try {
                storage.buckets().get(bucketName).execute();
            } catch (Exception e) {
                String message = String.format("The specified bucket with %s name does not exist", bucketName);
                LOGGER.debug(message + ":" + e.getMessage());
                resultBuilder.error(message);
            }
        }
        SpiFileSystem spiFileSystem = request.getSpiFileSystem();
        try {
            resultBuilder = gcpServiceAccountObjectStorageValidator.validateObjectStorage(
                    request.getCredential(),
                    spiFileSystem,
                    resultBuilder);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage());
            resultBuilder.error(e.getMessage());
        }
        ValidationResult validationResult = resultBuilder.build();
        if (validationResult.hasError()) {
            return ObjectStorageValidateResponse.builder()
                    .withStatus(ResponseStatus.ERROR)
                    .withError(validationResult.getFormattedErrors())
                    .build();
        }
        return ObjectStorageValidateResponse.builder()
                .withStatus(ResponseStatus.OK)
                .build();
    }

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}
