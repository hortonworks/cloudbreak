package com.sequenceiq.cloudbreak.client;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.glassfish.jersey.SslConfigurator;
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
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = {trustEverythingTrustManager()};
        try {
            // Install the all-trusting trust manager
            SSLContext sc = SslConfigurator.newInstance().createSSLContext();
            sc.init(null, trustAllCerts, new SecureRandom());
            LOGGER.debug("Trust all SSL certificates has been installed");
            return sc;
        } catch (KeyManagementException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException("F", e);
        }
    }

    private static X509TrustManager trustEverythingTrustManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                LOGGER.debug("accept all issuer");
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                LOGGER.debug("checkClientTrusted");
                // Trust everything
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                LOGGER.debug("checkServerTrusted");
                // Trust everything
            }
        };
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