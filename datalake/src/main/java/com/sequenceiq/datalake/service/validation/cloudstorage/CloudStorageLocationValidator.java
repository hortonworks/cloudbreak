package com.sequenceiq.datalake.service.validation.cloudstorage;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ObjectStorageType;
import com.sequenceiq.datalake.service.validation.converter.CredentialResponseToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageLocationValidator {

    private final CredentialResponseToCloudCredentialConverter credentialResponseToCloudCredentialConverter;

    private final SecretService secretService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint;

    public CloudStorageLocationValidator(CredentialResponseToCloudCredentialConverter credentialResponseToCloudCredentialConverter,
            SecretService secretService, CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint) {
        this.credentialResponseToCloudCredentialConverter = credentialResponseToCloudCredentialConverter;
        this.secretService = secretService;
        this.cloudProviderServicesV4Endopint = cloudProviderServicesV4Endopint;
    }

    public void validate(String storageLocation, FileSystemType fileSystemType, DetailedEnvironmentResponse environment, ValidationResultBuilder resultBuilder) {
        String bucketName = getBucketName(fileSystemType, storageLocation);
        CloudCredential cloudCredential = credentialResponseToCloudCredentialConverter.convert(environment.getCredential());
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(environment.getCloudPlatform(), cloudCredential, bucketName,
                environment.getLocation().getName());
        ObjectStorageMetadataResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> cloudProviderServicesV4Endopint.getObjectStorageMetaData(request));
        resultBuilder.ifError(() -> response.getStatus() == ResponseStatus.OK && !environment.getLocation().getName().equals(response.getRegion()),
                String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                        response.getRegion(),
                        bucketName,
                        environment.getLocation().getName()));
        resultBuilder.ifError(() -> response.getStatus() == ResponseStatus.RESOURCE_NOT_FOUND,
                String.format("Object storage cannot be found at location: %s.", bucketName));
    }

    private String getBucketName(FileSystemType fileSystemType, String storageLocation) {
        storageLocation = storageLocation.replace(fileSystemType.getProtocol() + "://", "");
        return storageLocation.split("/")[0];
    }

    private ObjectStorageMetadataRequest createObjectStorageMetadataRequest(String cloudPlatform, CloudCredential credential, String objectStoragePath,
            String regionName) {
        return ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(cloudPlatform)
                .withCredential(credential)
                .withObjectStoragePath(objectStoragePath)
                .withRegion(regionName)
                .withObjectStorageType(ObjectStorageType.DATALAKE)
                .build();
    }

}
