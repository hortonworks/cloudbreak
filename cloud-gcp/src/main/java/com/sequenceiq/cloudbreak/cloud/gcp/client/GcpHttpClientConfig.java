package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Objects;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.SecurityUtils;

@Configuration
public class GcpHttpClientConfig {

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return new NetHttpTransport.Builder()
                .trustCertificates(getCertificateTrustStore())
                .build();
    }

    private KeyStore getCertificateTrustStore() throws IOException, GeneralSecurityException {
        KeyStore certTrustStore = SecurityUtils.getDefaultKeyStore();
        try (InputStream keyStoreStream = GoogleUtils.class.getResourceAsStream("google.jks")) {
            SecurityUtils.loadKeyStore(certTrustStore, Objects.requireNonNull(keyStoreStream), "notasecret");
        }
        return certTrustStore;
    }
}
