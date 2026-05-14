package com.sequenceiq.cloudbreak.clusterproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ClusterProxySecretProviderTest {

    private final ClusterProxySecretProvider underTest = new ClusterProxySecretProvider();

    @Test
    void generateClusterProxySecretFormat() {
        String secretJson = "{\"enginePath\":\"secret\"," +
                "\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"cb/secretPath\"}";

        String result = underTest.generateClusterProxySecretFormat(secretJson);

        assertEquals("cb/secretPath:secret:TEXT", result);
    }

    @Test
    void generateSignKeys() {
        ClusterProxyCertificate certificate = underTest.generateSignKeys();

        assertNotNull(certificate.getSignKey());
        assertNotNull(certificate.getSignPub());
        assertNotNull(certificate.getSignCert());
    }
}
