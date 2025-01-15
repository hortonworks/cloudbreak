package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
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
        LOGGER.debug("Trying to load Google's certificates to default key store with type: {}", certTrustStore.getType());
        if ("bcfks".equals(certTrustStore.getType())) {
            LOGGER.warn("BCFKS key/trust stores not supported yet");
            certTrustStore.load(null);
        } else {
            LOGGER.debug("Loading Google's certificates into default key store with type: {}", certTrustStore.getType());
            try (InputStream keyStoreStream = GoogleUtils.class.getResourceAsStream("google.p12")) {
                SecurityUtils.loadKeyStore(certTrustStore, Objects.requireNonNull(keyStoreStream), "notasecret");
            }
        }
        return certTrustStore;
    }
}
