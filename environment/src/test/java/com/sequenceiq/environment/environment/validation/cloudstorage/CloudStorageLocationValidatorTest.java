package com.sequenceiq.environment.environment.validation.cloudstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;
import com.sequenceiq.environment.environment.dto.telemetry.WasbCloudStorageParameters;

@RunWith(MockitoJUnitRunner.class)
public class CloudStorageLocationValidatorTest {

    private static final String BUCKET_NAME = "bucket-name";

    private static final String OBJECT_PATH = "bucket-name/folder/file";

    private static final String S3_OBJECT_PATH = "s3a://bucket-name/folder/file";

    private static final String WASB_OBJECT_PATH = "wasb://bucket-name/folder/file";

    private static final String ENV_REGION = "env-region";

    private static final String OTHER_REGION = "other-region";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential("id", "name");

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private Environment environment;

    @Mock
    private CloudProviderServicesV4Endopint cloudProviderServicesEndpoint;

    @Mock
    private EnvironmentTelemetry environmentTelemetry;

    @Mock
    private EnvironmentLogging environmentLogging;

    @Mock
    private S3CloudStorageParameters s3;

    @Mock
    private WasbCloudStorageParameters wasb;

    @InjectMocks
    private CloudStorageLocationValidator underTest;

    @Before
    public void setUp() {
        when(environment.getLocation()).thenReturn(ENV_REGION);
        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(environment.getCredential()).thenReturn(new Credential());
        when(environment.getTelemetry()).thenReturn(environmentTelemetry);
        when(environmentTelemetry.getLogging()).thenReturn(environmentLogging);
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(CLOUD_CREDENTIAL);
    }

    @Test
    public void validateS3() {
        when(environmentLogging.getS3()).thenReturn(s3);
        when(s3.getType()).thenReturn(FileSystemType.S3);

        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(S3_OBJECT_PATH, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateWasb() {
        when(environmentLogging.getWasb()).thenReturn(wasb);
        when(wasb.getType()).thenReturn(FileSystemType.WASB);

        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(WASB_OBJECT_PATH, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateNoProtocol() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(OBJECT_PATH, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateError() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(OTHER_REGION).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(OBJECT_PATH, environment, validationResultBuilder);
        ValidationResult result = validationResultBuilder.build();

        assertTrue(result.hasError());
        assertEquals(String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                OTHER_REGION, BUCKET_NAME, ENV_REGION),
                result.getErrors().get(0));
    }
}
