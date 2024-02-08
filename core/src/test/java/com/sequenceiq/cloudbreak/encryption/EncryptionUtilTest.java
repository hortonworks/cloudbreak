package com.sequenceiq.cloudbreak.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CryptoConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilTest {

    @Mock
    private CryptoConnector cryptoConnector;

    @Mock
    private CredentialToCloudCredentialConverter cloudCredentialConverter;

    @Mock
    private SecretService secretService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @InjectMocks
    private EncryptionUtil underTest;

    @Captor
    private ArgumentCaptor<EncryptRequest> encryptRequestArgumentCaptor;

    private CloudCredential cloudCredential;

    private SecretResponse secretResponse;

    private DetailedEnvironmentResponse environment;

    @BeforeEach
    void setup() {
        cloudCredential = new CloudCredential("cloud-credential-id", "cloud-credential-name", "cloud-credential-account");
        secretResponse = new SecretResponse();
        environment = DetailedEnvironmentResponse.builder()
                .withCrn("environment-crn")
                .withEnableSecretEncryption(true)
                .withAws(AwsEnvironmentParameters.builder()
                        .withAwsDiskEncryptionParameters(AwsDiskEncryptionParameters.builder()
                                .withEncryptionKeyArn("keyArn")
                                .build())
                        .build())
                .withCredential(CredentialResponse.builder()
                        .withCloudPlatform(CloudPlatform.AWS.name())
                        .withName("cred")
                        .withAttributes(secretResponse)
                        .withCrn("cred-crn")
                        .withAccountId("accountid")
                        .build())
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse()
                        .withName("us-gov-west-1")
                        .build())
                .build();
    }

    @Test
    void testEncrypt() {
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        when(cloudCredentialConverter.convert(any())).thenReturn(cloudCredential);
        when(cloudConnector.cryptoConnector()).thenReturn(cryptoConnector);
        when(secretService.getByResponse(any())).thenReturn("attributes");
        when(cloudPlatformConnectors.get(Platform.platform(CloudPlatform.AWS.name()), Variant.variant(CloudPlatform.AWS.name()))).thenReturn(cloudConnector);
        when(cryptoConnector.encrypt(encryptRequestArgumentCaptor.capture())).thenReturn("encrypted-secret");

        String encryptResult = underTest.encrypt(AwsConstants.AWS_PLATFORM, AwsConstants.AWS_DEFAULT_VARIANT, "secret", environment, "SECRET_NAME");

        EncryptionKeySource expectedKeySource = EncryptionKeySource.builder()
                .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                .withKeyValue("keyArn")
                .build();
        Map<String, String> expectedEncryptionContext = Map.of("ENVIRONMENT_CRN", "environment-crn", "SECRET_NAME", "SECRET_NAME");

        assertEquals("encrypted-secret", encryptResult);
        verify(secretService, times(1)).getByResponse(eq(secretResponse));
        verify(cloudCredentialConverter, times(1)).convert(any());
        EncryptRequest capturedEncryptRequest = encryptRequestArgumentCaptor.getValue();
        assertEquals("secret", capturedEncryptRequest.input());
        assertEquals(expectedKeySource, capturedEncryptRequest.keySource());
        assertEquals(cloudCredential, capturedEncryptRequest.cloudCredential());
        assertEquals("us-gov-west-1", capturedEncryptRequest.regionName());
        assertEquals(expectedEncryptionContext, capturedEncryptRequest.encryptionContext());
    }

    @Test
    void testThrowIfSecretEncryptionNotAvailable() {
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        when(cloudPlatformConnectors.get(Platform.platform(CloudPlatform.AZURE.name()), Variant.variant(CloudPlatform.AZURE.name())))
                .thenReturn(cloudConnector);

        assertThrows(CloudConnectorException.class, () ->
                underTest.encrypt(AzureConstants.PLATFORM, AzureConstants.VARIANT, "secret", environment, "SECRET_NAME"));
    }

    @Test
    void testgetEncryptionKeySource() {
        environment.setCloudPlatform(CloudPlatform.AWS.name());

        EncryptionKeySource encryptionKeySource = underTest.getEncryptionKeySource(CloudPlatform.AWS, environment);

        assertEquals(EncryptionKeyType.AWS_KMS_KEY_ARN, encryptionKeySource.keyType());
        assertEquals("keyArn", encryptionKeySource.keyValue());
    }

    @Test
    void testgetEncryptionKeySourceWithUnknownCloudPlatform() {
        environment.setCloudPlatform(CloudPlatform.AZURE.name());

        assertThrows(CloudConnectorException.class, () -> underTest.getEncryptionKeySource(CloudPlatform.AZURE, environment));
    }

}
