package com.sequenceiq.datalake.service.validation.cloudstorage;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
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

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final SecretService secretService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint;

    public CloudStorageLocationValidator(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            SecretService secretService, CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.secretService = secretService;
        this.cloudProviderServicesV4Endopint = cloudProviderServicesV4Endopint;
    }

    public void validate(String storageLocation, FileSystemType fileSystemType, DetailedEnvironmentResponse environment, ValidationResultBuilder resultBuilder) {
        String bucketName = getBucketName(fileSystemType, storageLocation);
        Credential credential = getCredential(environment);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(environment.getCloudPlatform(), cloudCredential, bucketName);
        ObjectStorageMetadataResponse response = cloudProviderServicesV4Endopint.getObjectStorageMetaData(request);
        resultBuilder.ifError(() -> response.getStatus() == ResponseStatus.OK && !environment.getLocation().getName().equals(response.getRegion()),
                String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                        response.getRegion(),
                        bucketName,
                        environment.getLocation().getName()));
    }

    private String getBucketName(FileSystemType fileSystemType, String storageLocation) {
        storageLocation = storageLocation.replace(fileSystemType.getProtocol() + "://", "");
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
