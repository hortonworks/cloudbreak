package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.SecurityUtils;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

@Configuration
public class GcpHttpClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpHttpClientConfig.class);

    private static final String CIPHER_SUITE_SEPARATOR = ":";

    private static final String[] TLS_PROTOCOLS = {TLS_1_3.getVersion()};

    // google.jks ships inside google-api-client and is a JKS keystore (magic bytes FEEDFEED), despite
    // historically being read through a PKCS12 keystore here: the SUN provider's PKCS12 implementation
    // transparently accepts JKS. jdk-25-fips drops that provider, so it must be read as JKS. See CB-35226.
    private static final String GOOGLE_TRUST_BUNDLE = "google.jks";

    private static final String GOOGLE_TRUST_BUNDLE_PASSWORD = "notasecret";

    @Value("${cb.gcp.tlsHardening:false}")
    private boolean tlsHardeningEnabled;

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        NetHttpTransport.Builder builder = new NetHttpTransport.Builder()
                .trustCertificates(getCertificateTrustStore());
        applyTlsHardeningIfEnabled(builder);
        return builder.build();
    }

    private void applyTlsHardeningIfEnabled(NetHttpTransport.Builder builder) {
        if (tlsHardeningEnabled) {
            String[] ciphers = resolveCipherSuites();
            builder.setSslSocketConfigurator(socket -> {
                socket.setEnabledProtocols(TLS_PROTOCOLS);
                socket.setEnabledCipherSuites(ciphers);

            });
            LOGGER.info("Initialising GCP HTTP client with TLS protocols={} ciphers={}",
                    Arrays.toString(TLS_PROTOCOLS), Arrays.toString(ciphers));
        } else {
            LOGGER.info("GCP HTTP client TLS hardening is disabled");
        }
    }

    private String[] resolveCipherSuites() {
        String cipherSuites = encryptionProfileProvider.getTls13RecommendedCipherSuites(true);
        LOGGER.debug("TLS 1.3 recommended cipher suites: {}", cipherSuites);
        return StringUtils.isBlank(cipherSuites) ? new String[0] : cipherSuites.split(CIPHER_SUITE_SEPARATOR);
    }

    private KeyStore getCertificateTrustStore() throws IOException, GeneralSecurityException {
        KeyStore certTrustStore = SecurityUtils.getDefaultKeyStore();
        try (InputStream keyStoreStream = GoogleUtils.class.getResourceAsStream(GOOGLE_TRUST_BUNDLE)) {
            certTrustStore.load(null, null);
            KeyStore googleTrustBundle = SecurityUtils.getJavaKeyStore();
            SecurityUtils.loadKeyStore(googleTrustBundle, Objects.requireNonNull(keyStoreStream), GOOGLE_TRUST_BUNDLE_PASSWORD);
            int certificates = 0;
            for (String alias : Collections.list(googleTrustBundle.aliases())) {
                if (!googleTrustBundle.isKeyEntry(alias)) {
                    certTrustStore.setCertificateEntry(alias, googleTrustBundle.getCertificate(alias));
                    certificates++;
                }
            }
            LOGGER.info("Loaded {} of Google's certificates into the {} trust store", certificates, certTrustStore.getType());
        } catch (Exception e) {
            // Non-fatal on purpose: this bean is built even where GCP is disabled, so throwing would block startup.
            LOGGER.warn("Could not load Google's certificates into the {} trust store, continuing with an empty one. "
                    + "GCP API calls will fail the TLS handshake. Expected where GCP is not an enabled platform.",
                    certTrustStore.getType(), e);
            certTrustStore.load(null);
        }
        return certTrustStore;
    }
}
