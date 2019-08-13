package com.sequenceiq.datalake.service.validation.cloudstorage;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.Credential;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageLocationValidator {

    private final CloudbreakServiceUserCrnClient cloudbreakServiceUserCrnClient;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final SecretService secretService;

    public CloudStorageLocationValidator(CloudbreakServiceUserCrnClient cloudbreakServiceUserCrnClient,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            SecretService secretService) {
        this.cloudbreakServiceUserCrnClient = cloudbreakServiceUserCrnClient;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.secretService = secretService;
    }

    public void validate(String userCrn, String storageLocation, DetailedEnvironmentResponse environment, ValidationResultBuilder resultBuilder) {
        String bucketName = getBucketName(storageLocation);
        Credential credential = getCredential(environment);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(environment.getCloudPlatform(), cloudCredential, bucketName);
        ObjectStorageMetadataResponse response = cloudbreakServiceUserCrnClient.withCrn(userCrn).cloudProviderServicesEndpoint()
                .getObjectStorageMetaData(request);
        resultBuilder.ifError(() -> !environment.getLocation().getName().equals(response.getRegion()),
                String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                        response.getRegion(),
                        bucketName,
                        environment.getLocation()));
    }

    private String getBucketName(String storageLocation) {
        storageLocation = storageLocation.replace(FileSystemType.S3.getProtocol() + "://", "");
        return storageLocation.split("/")[0];
    }

    private ObjectStorageMetadataRequest createObjectStorageMetadataRequest(String cloudPlatform, CloudCredential credential, String objectStoragePath) {
        return ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(cloudPlatform)
                .withCredential(credential)
                .withObjectStoragePath(objectStoragePath)
                .build();
    }

    private Credential getCredential(DetailedEnvironmentResponse environment) {
        return new Credential(environment.getCloudPlatform(),
                environment.getCredential().getName(),
                secretService.getByResponse(environment.getCredential().getAttributes()),
                environment.getCredential().getCrn());
    }
}
