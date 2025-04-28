package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.SecurityUtils;

@Configuration
public class GcpHttpClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpHttpClientConfig.class);

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return new NetHttpTransport.Builder()
                .trustCertificates(getCertificateTrustStore())
                .build();
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
