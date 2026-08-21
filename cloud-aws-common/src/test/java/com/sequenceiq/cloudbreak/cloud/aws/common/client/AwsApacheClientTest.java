package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

@ExtendWith(MockitoExtension.class)
public class AwsApacheClientTest {

    private static final String RECOMMENDED_TLS_13_CIPHERS = "TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384";

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    private AwsApacheClient underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AwsApacheClient();
        ReflectionTestUtils.setField(underTest, "encryptionProfileProvider", encryptionProfileProvider);
        ReflectionTestUtils.setField(underTest, "maxConnections", 500);
    }

    @Test
    public void testInitBuildsHardenedClientWhenHardeningEnabled() {
        ReflectionTestUtils.setField(underTest, "tlsHardeningEnabled", true);
        when(encryptionProfileProvider.getTls13RecommendedCipherSuites(true)).thenReturn(RECOMMENDED_TLS_13_CIPHERS);

        underTest.init();

        assertThat(underTest.getApacheHttpClient()).isNotNull();
        verify(encryptionProfileProvider).getTls13RecommendedCipherSuites(true);
    }

    @Test
    public void testInitBuildsPlainClientWhenHardeningDisabled() {
        ReflectionTestUtils.setField(underTest, "tlsHardeningEnabled", false);

        underTest.init();

        assertThat(underTest.getApacheHttpClient()).isNotNull();
        verifyNoInteractions(encryptionProfileProvider);
    }

    @Test
    public void testInitToleratesEmptyCipherSuites() {
        ReflectionTestUtils.setField(underTest, "tlsHardeningEnabled", true);
        when(encryptionProfileProvider.getTls13RecommendedCipherSuites(true)).thenReturn("");

        underTest.init();

        assertThat(underTest.getApacheHttpClient()).isNotNull();
    }
}
