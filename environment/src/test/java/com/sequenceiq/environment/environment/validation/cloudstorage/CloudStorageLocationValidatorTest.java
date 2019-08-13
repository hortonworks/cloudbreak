package com.sequenceiq.environment.environment.validation.cloudstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CloudStorageLocationValidatorTest {

    private static final String BUCKET_NAME = "bucket-name";

    private static final String OBJECT_PATH = "bucket-name/folder/file";

    private static final String ENV_REGION = "env-region";

    private static final String OTHER_REGION = "other-region";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential("id", "name");

    private static final String USER_CRN = "userCrn";

    @Mock
    private CloudbreakServiceUserCrnClient cloudbreakServiceUserCrnClient;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private Environment environment;

    @Mock
    private CloudProviderServicesV4Endopint cloudProviderServicesEndpoint;

    @InjectMocks
    private CloudStorageLocationValidator underTest;

    @Before
    public void setUp() {
        when(environment.getLocation()).thenReturn(ENV_REGION);
        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(environment.getCredential()).thenReturn(new Credential());
        when(cloudbreakServiceUserCrnClient.withCrn(anyString())).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.cloudProviderServicesEndpoint()).thenReturn(cloudProviderServicesEndpoint);
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(CLOUD_CREDENTIAL);
    }

    @Test
    public void validate() {
        ObjectStorageMetadataRequest request = ObjectStorageMetadataRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(CLOUD_CREDENTIAL)
                .withObjectStoragePath(BUCKET_NAME)
                .build();
        ObjectStorageMetadataResponse response = ObjectStorageMetadataResponse.builder().withRegion(ENV_REGION).build();
        when(cloudProviderServicesEndpoint.getObjectStorageMetaData(eq(request))).thenReturn(response);

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(USER_CRN, OBJECT_PATH, environment, validationResultBuilder);

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
        underTest.validate(USER_CRN, OBJECT_PATH, environment, validationResultBuilder);
        ValidationResult result = validationResultBuilder.build();

        assertTrue(result.hasError());
        assertEquals(String.format("Object storage location [%s] of bucket '%s' must match environment location [%s]",
                OTHER_REGION, BUCKET_NAME, ENV_REGION),
                result.getErrors().get(0));
    }
}
