package com.sequenceiq.cloudbreak.cloud.aws.common.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.cryptography.materialproviders.IKeyring;

@ExtendWith(MockitoExtension.class)
class AwsEncryptionSdkCryptoConnectorTest {

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private Retry retryService;

    @Mock
    private AwsCrypto awsCrypto;

    @InjectMocks
    private AwsEncryptionSdkCryptoConnector underTest;

    @Captor
    private ArgumentCaptor<AwsCredentialView> awsCredentialViewArgumentCaptor;

    @Test
    void testEncrypt() {
        CloudCredential cloudCredential = new CloudCredential("ccid", "ccname", "ccaccount");
        Map<String, String> encryptionContext = Map.of("k1", "v1", "k2", "v2");
        EncryptionKeySource encryptionKeySource = EncryptionKeySource.builder()
                .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                .withKeyValue("keyArn")
                .build();
        EncryptRequest encryptRequest = EncryptRequest.builder()
                .withInput("secret")
                .withKeySource(encryptionKeySource)
                .withCloudCredential(cloudCredential)
                .withRegionName("region")
                .withEncryptionContext(encryptionContext)
                .build();
        CryptoResult cryptoResult = mock(CryptoResult.class);
        KmsClient kmsClient = mock(KmsClient.class);
        when(commonAwsClient.createKmsClient(awsCredentialViewArgumentCaptor.capture(), eq("region"))).thenReturn(kmsClient);
        when(awsCrypto.encryptData(any(IKeyring.class), eq("secret".getBytes()), eq(encryptionContext))).thenReturn(cryptoResult);
        when(cryptoResult.getResult()).thenReturn("encrypted-secret".getBytes());
        when(retryService.testWith1SecDelayMax5Times(any())).thenAnswer(invocation -> {
            Supplier<?> supplierArgument = invocation.getArgument(0);
            return supplierArgument.get();
        });

        String encryptionResult = underTest.encrypt(encryptRequest);

        AwsCredentialView capturedAwsCredentialView = awsCredentialViewArgumentCaptor.getValue();
        verify(commonAwsClient, times(1)).createKmsClient(capturedAwsCredentialView, "region");
        assertEquals("ccid", capturedAwsCredentialView.getId());
        assertEquals("ccname", capturedAwsCredentialView.getName());
        assertEquals("encrypted-secret", Base64Util.decode(encryptionResult));

    }
}
