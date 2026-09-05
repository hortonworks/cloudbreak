package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

@ExtendWith(MockitoExtension.class)
public class AzureHttpClientConfigurerTest {

    private static final String RECOMMENDED_TLS_13_CIPHERS = "TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384";

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @Test
    public void testHardenedClientWhenHardeningEnabled() {
        when(encryptionProfileProvider.getTls13RecommendedCipherSuites(true)).thenReturn(RECOMMENDED_TLS_13_CIPHERS);

        AzureHttpClientConfigurer underTest = new AzureHttpClientConfigurer(
                HttpLogDetailLevel.BASIC, true, Executors.newSingleThreadExecutor(), new SimpleMeterRegistry(), encryptionProfileProvider);

        verify(encryptionProfileProvider).getTls13RecommendedCipherSuites(true);
        OkHttpClient okHttpClient = extractOkHttpClient(underTest);
        List<ConnectionSpec> specs = okHttpClient.connectionSpecs();
        assertThat(specs).hasSize(1);
        ConnectionSpec spec = specs.getFirst();
        assertThat(spec.tlsVersions()).containsExactly(TlsVersion.TLS_1_3);
        assertThat(spec.cipherSuites()).containsExactlyInAnyOrder(
                CipherSuite.forJavaName("TLS_AES_128_GCM_SHA256"),
                CipherSuite.forJavaName("TLS_AES_256_GCM_SHA384"));
    }

    @Test
    public void testPlainClientWhenHardeningDisabled() {
        AzureHttpClientConfigurer underTest = new AzureHttpClientConfigurer(
                HttpLogDetailLevel.BASIC, false, Executors.newSingleThreadExecutor(), new SimpleMeterRegistry(), encryptionProfileProvider);

        verifyNoInteractions(encryptionProfileProvider);
        OkHttpClient okHttpClient = extractOkHttpClient(underTest);
        assertThat(okHttpClient.connectionSpecs()).containsExactly(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT);
    }

    private static OkHttpClient extractOkHttpClient(AzureHttpClientConfigurer configurer) {
        return (OkHttpClient) ReflectionTestUtils.getField(configurer, "okHttpClient");
    }
}
