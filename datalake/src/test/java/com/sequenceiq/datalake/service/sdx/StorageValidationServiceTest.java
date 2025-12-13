package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageValidator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
class StorageValidationServiceTest {

    private static final String DEFAULT_BACKUP_LOCATION = "abfs://example-path";

    private static final String BACKUP_LOCATION = "abfs://backup@location/to/backup";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private CloudStorageValidator cloudStorageValidator;

    @Mock
    private CloudStorageManifester cloudStorageManifester;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @InjectMocks
    private StorageValidationService underTest;

    @Test
    public void whenAzureNotEvenConfiguredWithManagedIdentityShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation(DEFAULT_BACKUP_LOCATION);
        cloudStorageRequest.setFileSystemType(FileSystemType.ADLS_GEN_2);
        cloudStorageRequest.setAdlsGen2(null);

        assertThrows(BadRequestException.class,
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

        assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("AZURE", cloudStorageRequest));
    }

    @Test
    public void whenAwsNotEvenConfiguredWithRoleShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.S3);
        cloudStorageRequest.setS3(null);

        assertThrows(BadRequestException.class,
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

        assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("AWS", cloudStorageRequest));
    }

    @Test
    public void whenGcsNotEvenConfiguredWithServiceAccountShouldNotThrowException() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("gs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.GCS);
        cloudStorageRequest.setGcs(null);

        assertThrows(BadRequestException.class,
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

        assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage("GCP", cloudStorageRequest));
    }

    @Test
    public void throwErrorWhenS3LocationInvalid() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("cloudbreakbucket/something");
        S3CloudStorageV1Parameters params = new S3CloudStorageV1Parameters();
        params.setInstanceProfile("instanceProfile");
        cloudStorageRequest.setS3(params);

        BadRequestException exception = assertThrows(BadRequestException.class,
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
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorage(CloudPlatform.AWS.toString(), sdxCloudStorageRequest));
        assertEquals(exception.getMessage(), "instance profile must be defined for S3");
    }

    @Test
    public void validateBackupStorageSdxClusterFailure() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateBackupStorage(sdxCluster, BackupOperationType.ANY, null));
        assertEquals(exception.getMessage(), "Failed to validate backup storage");
    }

    @Test
    public void validateBackupStorageEnvironmentServiceFailure() {
        when(sdxCluster.getEnvName()).thenReturn("test environment");
        when(environmentService.getDetailedEnvironmentResponseByName(anyString())).thenReturn(new DetailedEnvironmentResponse());
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateBackupStorage(sdxCluster, BackupOperationType.ANY,  null));
        assertEquals(exception.getMessage(), "Failed to validate backup storage");
    }

    @Test
    public void validateBackupStorageEnvironmentSuccess() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase(), new StorageLocationBase()));
        cloudStorageRequest.setIdentities(List.of(new StorageIdentityBase(), new StorageIdentityBase()));
        when(cloudStorageManifester.initCloudStorageRequestFromExistingSdxCluster(any(), any())).thenReturn(cloudStorageRequest);
        StackV4Response stackV4Response = new StackV4Response();
        when(stackV4Endpoint.get(anyLong(), any(), anySet(), any())).thenReturn(stackV4Response);
        ArgumentCaptor<CloudStorageRequest> cloudStorageRequestArgumentCaptor = ArgumentCaptor.forClass(CloudStorageRequest.class);
        ArgumentCaptor<DetailedEnvironmentResponse> detailedEnvironmentResponseArgumentCaptor = ArgumentCaptor.forClass(DetailedEnvironmentResponse.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setParentEnvironmentName("test-environment");
        BackupResponse backupResponse = new BackupResponse();
        backupResponse.setStorageLocation(DEFAULT_BACKUP_LOCATION);
        detailedEnvironmentResponse.setBackup(backupResponse);
        when(sdxCluster.getEnvName()).thenReturn("test environment");
        when(environmentService.getDetailedEnvironmentResponseByName(anyString())).thenReturn(detailedEnvironmentResponse);
        ValidationResult validationResult = underTest.validateBackupStorage(sdxCluster, BackupOperationType.ANY, null);
        verify(cloudStorageValidator, times(1)).validateBackupLocation(cloudStorageRequestArgumentCaptor.capture(),
                eq(BackupOperationType.ANY), detailedEnvironmentResponseArgumentCaptor.capture(), eq(null), any(), any());
        assertEquals(2, cloudStorageRequestArgumentCaptor.getValue().getLocations().size());
        assertEquals(2, cloudStorageRequestArgumentCaptor.getValue().getIdentities().size());
        assertNull(cloudStorageRequestArgumentCaptor.getValue().getAws());
        assertEquals("test-environment", detailedEnvironmentResponseArgumentCaptor.getValue().getParentEnvironmentName());
        assertEquals(DEFAULT_BACKUP_LOCATION, detailedEnvironmentResponseArgumentCaptor.getValue().getBackupLocation());
        assertEquals(ValidationResult.State.VALID, validationResult.getState());
    }

    @Test
    public void validateBackupStorageEnvironmentSuccessWithNonDefaultLocation() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase(), new StorageLocationBase()));
        cloudStorageRequest.setIdentities(List.of(new StorageIdentityBase(), new StorageIdentityBase()));
        when(cloudStorageManifester.initCloudStorageRequestFromExistingSdxCluster(any(), any())).thenReturn(cloudStorageRequest);
        StackV4Response stackV4Response = new StackV4Response();
        when(stackV4Endpoint.get(anyLong(), any(), anySet(), any())).thenReturn(stackV4Response);
        ArgumentCaptor<CloudStorageRequest> cloudStorageRequestArgumentCaptor = ArgumentCaptor.forClass(CloudStorageRequest.class);
        ArgumentCaptor<DetailedEnvironmentResponse> detailedEnvironmentResponseArgumentCaptor = ArgumentCaptor.forClass(DetailedEnvironmentResponse.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setParentEnvironmentName("test-environment");
        when(sdxCluster.getEnvName()).thenReturn("test environment");
        when(environmentService.getDetailedEnvironmentResponseByName(anyString())).thenReturn(detailedEnvironmentResponse);
        ValidationResult validationResult = underTest.validateBackupStorage(sdxCluster, BackupOperationType.ANY, BACKUP_LOCATION);
        verify(cloudStorageValidator, times(1)).validateBackupLocation(cloudStorageRequestArgumentCaptor.capture(),
                eq(BackupOperationType.ANY), detailedEnvironmentResponseArgumentCaptor.capture(), eq(BACKUP_LOCATION), any(), any());
        assertEquals(2, cloudStorageRequestArgumentCaptor.getValue().getLocations().size());
        assertEquals(2, cloudStorageRequestArgumentCaptor.getValue().getIdentities().size());
        assertNull(cloudStorageRequestArgumentCaptor.getValue().getAws());
        assertEquals("test-environment", detailedEnvironmentResponseArgumentCaptor.getValue().getParentEnvironmentName());
        assertEquals(ValidationResult.State.VALID, validationResult.getState());
    }

    public static Stream<Arguments> storageBaseLocationsWhiteSpaceValidation() {
        return Stream.of(
                Arguments.of(" abfs://myscontainer@mystorage", ValidationResult.State.VALID),
                Arguments.of("abfs://myscontainer @mystorage ", ValidationResult.State.ERROR),
                Arguments.of("a bfs://myscontainer@mystorage ", ValidationResult.State.ERROR),
                Arguments.of("s3a://mybucket/mylocation      ", ValidationResult.State.VALID),
                Arguments.of("abfs://myscontainer@mystorage ", ValidationResult.State.VALID));
    }

    @ParameterizedTest
    @MethodSource("storageBaseLocationsWhiteSpaceValidation")
    void testValidateBaseLocationWhenWhiteSpaceIsPresent(String input, ValidationResult.State expected) {
        ValidationResult result = underTest.validateBaseLocation(input);
        assertEquals(expected, result.getState());
    }
}
