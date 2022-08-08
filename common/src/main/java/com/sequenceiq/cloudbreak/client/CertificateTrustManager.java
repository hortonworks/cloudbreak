package com.sequenceiq.cloudbreak.client;

import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateTrustManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateTrustManager.class);

    private CertificateTrustManager() {
    }

    public static HostnameVerifier hostnameVerifier() {

        // Do not verify host names
        return (hostname, sslSession) -> {
            LOGGER.debug("verify hostname: {}", hostname);
            return true;
        };

    }

    public static SSLContext sslContext() {
        try {
            SSLContext defaultSslContext = SSLContext.getDefault();
            LOGGER.debug("Default SSL context has been initialised");
            return defaultSslContext;
        } catch (NoSuchAlgorithmException e) {
            String errorMessage = String.format("Failed to initialise SSL context due to: '%s'", e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public static class SavingX509TrustManager implements X509TrustManager {

        private X509Certificate[] chain;

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            this.chain = chain;
        }

        public X509Certificate[] getChain() {
            return chain;
        }
    }
}