package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Collections;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.SecurityUtils;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

@ExtendWith(MockitoExtension.class)
public class GcpHttpClientConfigTest {

    private static final String RECOMMENDED_TLS_13_CIPHERS = "TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384";

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @InjectMocks
    private GcpHttpClientConfig underTest;

    @Test
    public void testHttpTransport() throws GeneralSecurityException, IOException {
        NetHttpTransport expected = GoogleNetHttpTransport.newTrustedTransport();

        HttpTransport httpTransport = underTest.httpTransport();

        assertEquals(expected.getClass(), httpTransport.getClass());
        verifyNoInteractions(encryptionProfileProvider);
        assertThat(extractSslSocketFactory((NetHttpTransport) httpTransport))
                .as("SSLSocketFactory should not be wrapped when TLS hardening is disabled")
                .isNotInstanceOf(configurableSslSocketFactoryClass());
    }

    @Test
    public void testHttpTransportWithTlsHardeningEnabled() throws GeneralSecurityException, IOException {
        ReflectionTestUtils.setField(underTest, "tlsHardeningEnabled", true);
        when(encryptionProfileProvider.getTls13RecommendedCipherSuites(true)).thenReturn(RECOMMENDED_TLS_13_CIPHERS);

        HttpTransport httpTransport = underTest.httpTransport();

        verify(encryptionProfileProvider).getTls13RecommendedCipherSuites(true);
        assertThat(httpTransport).isInstanceOf(NetHttpTransport.class);
        SSLSocketFactory socketFactory = extractSslSocketFactory((NetHttpTransport) httpTransport);
        assertThat(socketFactory)
                .as("SSLSocketFactory should be wrapped with ConfigurableSSLSocketFactory when TLS hardening is enabled")
                .isInstanceOf(configurableSslSocketFactoryClass());
        SSLSocket sslSocket = mock(SSLSocket.class);
        invokeConfigurator(socketFactory, sslSocket);
        verify(sslSocket).setEnabledProtocols(new String[] {"TLSv1.3"});
        verify(sslSocket).setEnabledCipherSuites(new String[] {"TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"});
    }

    @Test
    public void testCertificateTrustStoreContainsGoogleCertificates() throws KeyStoreException {
        KeyStore trustStore = ReflectionTestUtils.invokeMethod(underTest, "getCertificateTrustStore");

        assertThat(trustStore).isNotNull();
        assertThat(certificateCount(trustStore))
                .as("Google's bundled google.jks is a JKS keystore, so it must load through getJavaKeyStore(). "
                        + "Reading it as PKCS12 only worked via the SUN provider and fails on jdk-25-fips (CB-35226).")
                .isPositive();
    }

    @Test
    public void testCertificateTrustStoreFallsBackToEmptyStoreWhenBundleCannotBeRead() throws KeyStoreException {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::getJavaKeyStore).thenThrow(new KeyStoreException("JKS not available"));

            KeyStore trustStore = ReflectionTestUtils.invokeMethod(underTest, "getCertificateTrustStore");

            // Must not propagate: this bean is constructed at startup wherever cloud-gcp is on the classpath,
            // including gov deployments where GCP is not an enabled platform (enabledgovplatforms: AWS).
            assertThat(trustStore)
                    .as("A load failure must leave the service startable, not fail bean construction")
                    .isNotNull();
            assertThat(certificateCount(trustStore)).isZero();
        }
    }

    private static int certificateCount(KeyStore keyStore) throws KeyStoreException {
        int certificates = 0;
        for (String alias : Collections.list(keyStore.aliases())) {
            if (!keyStore.isKeyEntry(alias)) {
                certificates++;
            }
        }
        return certificates;
    }

    private static SSLSocketFactory extractSslSocketFactory(NetHttpTransport transport) {
        return (SSLSocketFactory) ReflectionTestUtils.getField(transport, "sslSocketFactory");
    }

    private static Class<?> configurableSslSocketFactoryClass() {
        try {
            return Class.forName("com.google.api.client.http.javanet.ConfigurableSSLSocketFactory");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("google-http-client ConfigurableSSLSocketFactory not on classpath", e);
        }
    }

    private static void invokeConfigurator(SSLSocketFactory configurableSocketFactory, SSLSocket target) {
        Object configurator = ReflectionTestUtils.getField(configurableSocketFactory, "configurator");
        ReflectionTestUtils.invokeMethod(configurator, "configure", target);
    }
}
