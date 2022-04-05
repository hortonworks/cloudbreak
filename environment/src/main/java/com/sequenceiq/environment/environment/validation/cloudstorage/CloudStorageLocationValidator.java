package com.sequenceiq.environment.environment.validation.cloudstorage;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.exception.EnvironmentServiceException;

@Component
public class CloudStorageLocationValidator {

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public CloudStorageLocationValidator(CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.cloudProviderServicesV4Endopint = cloudProviderServicesV4Endopint;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void validate(String storageLocation, Environment environment, ValidationResultBuilder resultBuilder) {
        Optional<FileSystemType> fileSystemType = getFileSystemType(environment);
        if (fileSystemType.isPresent() && FileSystemType.GCS.equals(fileSystemType.get()) && !storageLocation.startsWith("gs://")) {
            throw new EnvironmentServiceException(String.format("The Google storage location [%s] should be a gs:// URL. %s",
                    storageLocation, getDocLink(environment.getCloudPlatform())));
        }
        String bucketName = getBucketName(fileSystemType, storageLocation);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(environment.getCloudPlatform(), cloudCredential, bucketName);
        ObjectStorageMetadataResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () ->
                cloudProviderServicesV4Endopint.getObjectStorageMetaData(request));
        resultBuilder.ifError(() -> response.getStatus() == ResponseStatus.OK && !environment.getLocation().equals(response.getRegion()),
                String.format("Object storage location [%s] of bucket '%s' must match environment location [%s].%s",
                        response.getRegion(),
                        bucketName,
                        environment.getLocation(),
                        getDocLink(environment.getCloudPlatform())));
    }

    private String getDocLink(String cloudPlatform) {
        String docReferenceLink = " Refer to Cloudera documentation at %s for the required rights.";
        if (cloudPlatform.equals(CloudConstants.AWS)) {
            return String.format(docReferenceLink, DocumentationLinkProvider.awsCloudStorageSetupLink());
        } else if (cloudPlatform.equals(CloudConstants.AZURE)) {
            return String.format(docReferenceLink, DocumentationLinkProvider.azureCloudStorageSetupLink());
        } else if (cloudPlatform.equals(CloudConstants.GCP)) {
            return String.format(docReferenceLink, DocumentationLinkProvider.googleCloudStorageSetupLink());
        }
        return "";
    }

    private Optional<FileSystemType> getFileSystemType(Environment environment) {
        Optional<FileSystemType> response = Optional.empty();
        if (environment.getTelemetry() != null && environment.getTelemetry().getLogging() != null) {
            EnvironmentLogging logging = environment.getTelemetry().getLogging();
            if (logging.getS3() != null) {
                return Optional.of(logging.getS3().getType());
            }
            if (logging.getAdlsGen2() != null) {
                return Optional.of(logging.getAdlsGen2().getType());
            }
            if (logging.getGcs() != null) {
                return Optional.of(logging.getGcs().getType());
            }
        }
        return response;
    }

    public void validateBackup(String storageLocation, Environment environment, ValidationResultBuilder resultBuilder) {
        Optional<FileSystemType> fileSystemType = getBackupFileSystemType(environment);
        String bucketName = getBucketName(fileSystemType, storageLocation);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        ObjectStorageMetadataRequest request = createObjectStorageMetadataRequest(environment.getCloudPlatform(), cloudCredential, bucketName);
        ObjectStorageMetadataResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () ->
                cloudProviderServicesV4Endopint.getObjectStorageMetaData(request));
        resultBuilder.ifError(() -> response.getStatus() == ResponseStatus.OK && !environment.getLocation().equals(response.getRegion()),
                String.format("Object storage location [%s] of bucket '%s' must match environment location [%s].%s",
                        response.getRegion(),
                        bucketName,
                        environment.getLocation(),
                        getDocLink(environment.getCloudPlatform())));
    }

    private Optional<FileSystemType> getBackupFileSystemType(Environment environment) {
        if (environment.getBackup() != null) {
            EnvironmentBackup backup = environment.getBackup();
            if (backup.getS3() != null) {
                return Optional.of(backup.getS3().getType());
            }
            if (backup.getAdlsGen2() != null) {
                return Optional.of(backup.getAdlsGen2().getType());
            }
            if (backup.getGcs() != null) {
                return Optional.of(backup.getGcs().getType());
            }
        }
        return Optional.empty();
    }

    private String getBucketName(Optional<FileSystemType> fileSystemType, String storageLocation) {
        if (fileSystemType.isPresent()) {
            storageLocation = storageLocation.replace(fileSystemType.get().getProtocol() + "://", "");
        }
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
