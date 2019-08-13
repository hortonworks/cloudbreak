package com.sequenceiq.environment.environment.validation.storagelocation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class AwsEnvironmentTelemetryLoggingStorageLocationValidator implements EnvironmentTelemetryLoggingStorageLocationValidator {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Override
    public void validate(String storageLocation, Environment environment, ValidationResultBuilder resultBuilder) {
        String bucketName = getBucketName(storageLocation);
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(bucketName);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        ObjectStorageConnector objectStorageConnector = getObjectStorageConnector(environment.getCloudPlatform());
        ObjectStorageMetadataResponse response = objectStorageConnector.getObjectStorageMetadata(cloudCredential, request);
        resultBuilder.ifError(() -> !environment.getLocation().equals(response.getRegion()),
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
        return storageLocation.split("/")[0];
    }
}
