package com.sequenceiq.it.cloudbreak.encryptionprofile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.encryptionprofile.EncryptionProfileAssertions;

public class EncryptionProfileAssertionsTest {

    @Test
    public void testValidateCipherSuitesConfigurationWithValidConfig() {
        EncryptionProfileAssertions underTest = new EncryptionProfileAssertions();
        String serviceName = "cdp-prometheus";
        String instanceIp = "192.168.1.100";
        String configContent = "tls_server_config:\n" +
                "  cert_file: /opt/cdp-prometheus/cdp-prometheus.crt\n" +
                "  key_file: /opt/cdp-prometheus/cdp-prometheus.key\n" +
                "  cipher_suites:\n" +
                "    - TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384\n" +
                "    - TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256\n" +
                "    - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384\n" +
                "    - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256\n" +
                "    - TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA\n" +
                "    - TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA\n" +
                "    - TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA\n" +
                "    - TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA\n" +
                "basic_auth_users:\n" +
                "  vmagent: $2y$10$RKSj4Ouf4pgzcLCvpLlX8.3UdTna7tbkInSGzly2s3OTqtZP.EijC";

        assertDoesNotThrow(() -> {
            underTest.validateCipherSuitesConfiguration(serviceName, configContent, instanceIp);
        });
    }
}