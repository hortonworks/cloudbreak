package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
class StorageValidationServiceTest {

    @InjectMocks
    private StorageValidationService underTest;

    @Test
    public void whenAzureNotEvenConfiguredWithManagedIdentityShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("abfs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.ADLS_GEN_2);
        cloudStorageRequest.setAdlsGen2(null);

        Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("AZURE", cloudStorageRequest));
    }

    @Test
    public void whenAzureConfiguredWithManagedIdentityShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("abfs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.ADLS_GEN_2);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity("managedidentity");
        cloudStorageRequest.setAdlsGen2(adlsGen2);

        underTest.validateCloudStorage("AZURE", cloudStorageRequest);
    }

    @Test
    public void whenAzureConfiguredWithoutManagedIdentityShouldThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("abfs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.ADLS_GEN_2);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity(null);
        cloudStorageRequest.setAdlsGen2(adlsGen2);

        Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("AZURE", cloudStorageRequest));
    }

    @Test
    public void whenAwsNotEvenConfiguredWithRoleShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.S3);
        cloudStorageRequest.setS3(null);

        Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("AWS", cloudStorageRequest));
    }

    @Test
    public void whenAwsConfiguredWithRoleShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.S3);
        S3CloudStorageV1Parameters s3 = new S3CloudStorageV1Parameters();
        s3.setInstanceProfile("role");
        cloudStorageRequest.setS3(s3);

        underTest.validateCloudStorage("AWS", cloudStorageRequest);
    }

    @Test
    public void whenAwsConfiguredWithoutRoleShouldThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.S3);
        S3CloudStorageV1Parameters s3 = new S3CloudStorageV1Parameters();
        s3.setInstanceProfile(null);
        cloudStorageRequest.setS3(s3);

        Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("AWS", cloudStorageRequest));
    }

    @Test
    public void whenGcsNotEvenConfiguredWithServiceAccountShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("gs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.GCS);
        cloudStorageRequest.setGcs(null);

        Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("GCP", cloudStorageRequest));
    }

    @Test
    public void whenGcsConfiguredWithServiceAccountShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("gs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.GCS);
        GcsCloudStorageV1Parameters gcs = new GcsCloudStorageV1Parameters();
        gcs.setServiceAccountEmail("mail");
        cloudStorageRequest.setGcs(gcs);

        underTest.validateCloudStorage("GCP", cloudStorageRequest);
    }

    @Test
    public void whenGcsConfiguredWithoutRoleShouldThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("gs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.GCS);
        GcsCloudStorageV1Parameters gcs = new GcsCloudStorageV1Parameters();
        gcs.setServiceAccountEmail(null);
        cloudStorageRequest.setGcs(gcs);

        Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("GCP", cloudStorageRequest));
    }

    @Test
    public void throwErrorWhenS3LocationInvalid() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("cloudbreakbucket/something");
        S3CloudStorageV1Parameters params = new S3CloudStorageV1Parameters();
        params.setInstanceProfile("instanceProfile");
        cloudStorageRequest.setS3(params);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage(CloudPlatform.AWS.toString(), cloudStorageRequest));
        assertEquals(exception.getMessage(), "AWS baselocation missing protocol. please specify s3a://");
    }

    @Test
    public void okWhenS3LocationIsValid() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://cloudbreakbucket/something");
        S3CloudStorageV1Parameters params = new S3CloudStorageV1Parameters();
        params.setInstanceProfile("instanceProfile");
        cloudStorageRequest.setS3(params);
        underTest.validateCloudStorage(CloudPlatform.AWS.toString(), cloudStorageRequest);
    }

    @Test
    public void whenInvalidConfigIsProvidedThrowBadRequest() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        SdxCloudStorageRequest sdxCloudStorageRequest = new SdxCloudStorageRequest();
        sdxCloudStorageRequest.setBaseLocation("s3a://example-path");
        sdxClusterRequest.setCloudStorage(sdxCloudStorageRequest);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage(CloudPlatform.AWS.toString(), sdxCloudStorageRequest));
        assertEquals(exception.getMessage(), "instance profile must be defined for S3");
    }

}