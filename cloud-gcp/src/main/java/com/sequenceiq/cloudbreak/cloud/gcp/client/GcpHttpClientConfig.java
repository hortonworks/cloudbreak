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
        try (InputStream keyStoreStream = GoogleUtils.class.getResourceAsStream("google.jks")) {
            LOGGER.debug("Trying to load Google's certificates to default key store with type: {}", certTrustStore.getType());
            certTrustStore.load(null, null);
            LOGGER.debug("Loading Google's certificates to PKCS12 key store");
            KeyStore pkcs12KeyStore = SecurityUtils.getPkcs12KeyStore();
            String googleKeyStoreDefaultPassword = "notasecret";
            SecurityUtils.loadKeyStore(pkcs12KeyStore, Objects.requireNonNull(keyStoreStream), googleKeyStoreDefaultPassword);
            LOGGER.debug("Loading certificates from PKCS12 key store to the default key store with type: {}", certTrustStore.getType());
            for (String alias : Collections.list(pkcs12KeyStore.aliases())) {
                if (pkcs12KeyStore.isKeyEntry(alias)) {
                    LOGGER.debug("Not ready to load keys to BCFKS trust store with alias: {}", alias);
                } else {
                    certTrustStore.setCertificateEntry(alias, pkcs12KeyStore.getCertificate(alias));
                }
            }
            LOGGER.debug("Loaded Google's certificates into default key store with type: {}", certTrustStore.getType());
        } catch (Exception e) {
            LOGGER.warn("Google's certificates could not be loaded into the default key store with type:'{}'.", certTrustStore.getType(), e);
            certTrustStore.load(null);
        }
        return certTrustStore;
    }
}
