package com.sequenceiq.environment.environment.validation.storagelocation;

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

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;

@RunWith(MockitoJUnitRunner.class)
public class AwsEnvironmentTelemetryLoggingStorageLocationValidatorTest {

    private static final String BUCKET_NAME = "bucket-name";

    private static final String OBJECT_PATH = "bucket-name/folder/file";

    private static final String ENV_REGION = "env-region";

    private static final String OTHER_REGION = "other-region";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private Environment environment;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private ObjectStorageConnector objectStorageConnector;

    @InjectMocks
    private AwsEnvironmentTelemetryLoggingStorageLocationValidator underTest;

    @Before
    public void setUp() {
    }

    @Test
    public void validate() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        when(environment.getLocation()).thenReturn(ENV_REGION);
        when(environment.getCredential()).thenReturn(new Credential());
        when(cloudPlatformConnectors.get(any(Platform.class), any(Variant.class))).thenReturn(cloudConnector);
        when(cloudConnector.objectStorage()).thenReturn(objectStorageConnector);
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(new CloudCredential("id", "name"));
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).build();
        when(objectStorageConnector.getObjectStorageMetadata(any(CloudCredential.class), eq(request))).thenReturn(response);

        underTest.validate(OBJECT_PATH, environment, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateError() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        when(environment.getLocation()).thenReturn(ENV_REGION);
        when(environment.getCredential()).thenReturn(new Credential());
        when(cloudPlatformConnectors.get(any(Platform.class), any(Variant.class))).thenReturn(cloudConnector);
        when(cloudConnector.objectStorage()).thenReturn(objectStorageConnector);
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(new CloudCredential("id", "name"));
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder().withObjectStoragePath(BUCKET_NAME).build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(OTHER_REGION).build();
        when(objectStorageConnector.getObjectStorageMetadata(any(CloudCredential.class), eq(request))).thenReturn(response);

        underTest.validate(OBJECT_PATH, environment, validationResultBuilder);

        ValidationResult result = validationResultBuilder.build();
        assertTrue(result.hasError());
        assertEquals(String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                OTHER_REGION, BUCKET_NAME, ENV_REGION),
                result.getErrors().get(0));
    }

    @Test
    public void getCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }
}
