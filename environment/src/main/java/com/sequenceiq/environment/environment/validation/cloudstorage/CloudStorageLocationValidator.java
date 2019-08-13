package com.sequenceiq.environment.environment.validation.cloudstorage;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class CloudStorageLocationValidator {

    private final CloudbreakServiceUserCrnClient cloudbreakClient;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public CloudStorageLocationValidator(CloudbreakServiceUserCrnClient cloudbreakClient,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.cloudbreakClient = cloudbreakClient;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public void validate(String userCrn, String storageLocation, Environment environment, ValidationResultBuilder resultBuilder) {
        String bucketName = getBucketName(storageLocation);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(environment.getCloudPlatform(), cloudCredential, bucketName);
        CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint = cloudbreakClient.withCrn(userCrn).cloudProviderServicesEndpoint();
        ObjectStorageMetadataResponse response = cloudProviderServicesV4Endopint.getObjectStorageMetaData(request);
        resultBuilder.ifError(() -> !environment.getLocation().equals(response.getRegion()),
                String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                        response.getRegion(),
                        bucketName,
                        environment.getLocation()));
    }

    private String getBucketName(String storageLocation) {
        return storageLocation.split("/")[0];
    }

    private ObjectStorageMetadataRequest createObjectStorageMetadataRequest(String cloudPlatform, CloudCredential credential, String objectStoragePath) {
        return ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(cloudPlatform)
                .withCredential(credential)
                .withObjectStoragePath(objectStoragePath)
                .build();
    }

}
