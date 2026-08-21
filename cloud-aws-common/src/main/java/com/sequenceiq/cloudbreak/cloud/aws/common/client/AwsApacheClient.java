package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

@Component
public class AwsApacheClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsApacheClient.class);

    private static final String CIPHER_SUITE_SEPARATOR = ":";

    private static final String[] TLS_PROTOCOLS = {TLS_1_3.getVersion()};

    private static final TlsKeyManagersProvider SYSTEM_KEY_MANAGERS_PROVIDER = SystemPropertyTlsKeyManagersProvider.create();

    @Value("${cb.aws.maxconnections:500}")
    private Integer maxConnections;

    @Value("${cb.aws.tlsHardening:false}")
    private boolean tlsHardeningEnabled;

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

    private SdkHttpClient sdkHttpClient;

    @PostConstruct
    void init() {
        ApacheHttpClient.Builder builder = ApacheHttpClient.builder().maxConnections(maxConnections);
        if (tlsHardeningEnabled) {
            String[] ciphers = resolveCipherSuites();
            SSLContext sslContext = buildSslContext();
            builder.socketFactory(new SSLConnectionSocketFactory(sslContext, TLS_PROTOCOLS, ciphers,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier()));
            LOGGER.info("Initialising AWS HTTP client with TLS protocols={} ciphers={}",
                    Arrays.toString(TLS_PROTOCOLS), Arrays.toString(ciphers));
        } else {
            LOGGER.info("tlsHardening is disabled");
        }
        sdkHttpClient = builder.build();
    }

    public SdkHttpClient getApacheHttpClient() {
        return sdkHttpClient;
    }

    private SSLContext buildSslContext() {
        try {
            KeyManager[] keyManagers = SYSTEM_KEY_MANAGERS_PROVIDER.keyManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);
            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new CloudbreakServiceException("Failed to build SSLContext for AWS HTTP client", e);
        }
    }

    private String[] resolveCipherSuites() {
        String cipherSuites = encryptionProfileProvider.getTls13RecommendedCipherSuites(true);
        LOGGER.debug("TLS 1.3 recommended cipher suites: {}", cipherSuites);
        return StringUtils.isBlank(cipherSuites) ? new String[0] : cipherSuites.split(CIPHER_SUITE_SEPARATOR);
    }
}
