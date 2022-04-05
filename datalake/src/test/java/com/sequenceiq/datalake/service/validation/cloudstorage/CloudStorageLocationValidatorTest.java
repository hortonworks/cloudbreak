package com.sequenceiq.datalake.service.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.Credential;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse.LocationResponseBuilder;

@ExtendWith(MockitoExtension.class)
public class CloudStorageLocationValidatorTest {
    private static final String BUCKET_NAME = "bucket-name";

    private static final String OBJECT_PATH = "bucket-name/folder/file";

    private static final String S3_OBJECT_PATH = "s3a://bucket-name/folder/file";

    private static final String WASB_OBJECT_PATH = "wasb://bucket-name/folder/file";

    private static final String ENV_REGION = "env-region";

    private static final String OTHER_REGION = "other-region";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential("id", "name", "account");

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private SecretService secretService;

    @Mock
    private CloudProviderServicesV4Endopint cloudProviderServicesEndpoint;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private CloudStorageLocationValidator underTest;

    @BeforeEach
    public void setUp() {
        LocationResponse locationResponse = LocationResponseBuilder.aLocationResponse().withName(ENV_REGION).build();
        when(environment.getLocation()).thenReturn(locationResponse);
        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(CLOUD_CREDENTIAL);
    }

    @Test
    public void validateS3() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).withStatus(ResponseStatus.OK).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(S3_OBJECT_PATH, FileSystemType.S3, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateWasb() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).withStatus(ResponseStatus.OK).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(WASB_OBJECT_PATH, FileSystemType.WASB, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateNoProtocol() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).withStatus(ResponseStatus.OK).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(OBJECT_PATH, FileSystemType.S3, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateAccessDenied() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withStatus(ResponseStatus.ACCESS_DENIED).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(OBJECT_PATH, FileSystemType.S3, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateError() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(OTHER_REGION).withStatus(ResponseStatus.OK).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(OBJECT_PATH, FileSystemType.S3, environment, validationResultBuilder);
        ValidationResult result = validationResultBuilder.build();

        assertTrue(result.hasError());
        assertEquals(String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                OTHER_REGION, BUCKET_NAME, ENV_REGION),
                result.getErrors().get(0));
    }
}
