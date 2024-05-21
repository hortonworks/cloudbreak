package com.sequenceiq.cloudbreak.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;

@ExtendWith(MockitoExtension.class)
class CryptoConnectorTest {

    @Spy
    private CryptoConnector underTest;

    @Test
    void testEncrypt() {
        EncryptionKeySource encryptionKeySource = EncryptionKeySource.builder()
                .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                .withKeyValue("keyArn")
                .build();
        CloudCredential cloudCredential = new CloudCredential("ccid", "ccname", "ccaccount");
        Map<String, String> encryptionContext = Map.of("k1", "v1", "k2", "v2");
        EncryptRequest encryptRequest = EncryptRequest.builder()
                .withInput("secret")
                .withKeySource(encryptionKeySource)
                .withCloudCredential(cloudCredential)
                .withRegionName("region")
                .withEncryptionContext(encryptionContext)
                .build();
        when(underTest.encrypt("secret".getBytes(), encryptionKeySource, cloudCredential, "region", encryptionContext))
                .thenReturn("encrypted-secret".getBytes());

        String encryptionResult = underTest.encrypt(encryptRequest);

        assertEquals("encrypted-secret", Base64Util.decode(encryptionResult));
        verify(underTest, times(1)).encrypt("secret".getBytes(), encryptionKeySource, cloudCredential, "region", encryptionContext);
    }
}
