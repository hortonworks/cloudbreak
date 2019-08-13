package com.sequenceiq.datalake.service.validation.cloudstorage;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.Credential;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class AwsCloudStorageLocationValidator implements CloudStorageLocationValidator {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private SecretService secretService;

    @Override
    public void validate(String storageLocation, DetailedEnvironmentResponse environment, ValidationResultBuilder resultBuilder) {
        String bucketName = getBucketName(storageLocation);
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(bucketName);
        Credential credential = new Credential(environment.getCloudPlatform(),
                environment.getCredential().getName(),
                secretService.getByResponse(environment.getCredential().getAttributes()),
                environment.getCredential().getCrn());
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        ObjectStorageConnector objectStorageConnector = getObjectStorageConnector(environment.getCloudPlatform());
        ObjectStorageMetadataResponse response = objectStorageConnector.getObjectStorageMetadata(cloudCredential, request);
        resultBuilder.ifError(() -> !environment.getLocation().getName().equals(response.getRegion()),
                String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                        response.getRegion(),
                        bucketName,
                        environment.getLocation()));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    private ObjectStorageMetadataRequest createObjectStorageMetadataRequest(String storageLocationPart) {
        return ObjectStorageMetadataRequest.builder()
                .withObjectStoragePath(storageLocationPart)
                .build();
    }

    private ObjectStorageConnector getObjectStorageConnector(String platform) {
        return cloudPlatformConnectors.get(Platform.platform(platform), Variant.variant(platform)).objectStorage();
    }

    private String getBucketName(String storageLocation) {
        storageLocation = storageLocation.replace(FileSystemType.S3.getProtocol() + "://", "");
        return storageLocation.split("/")[0];
    }
}
