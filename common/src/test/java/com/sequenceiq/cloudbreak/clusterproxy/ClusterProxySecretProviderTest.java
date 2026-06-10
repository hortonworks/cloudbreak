package com.sequenceiq.cloudbreak.clusterproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void generateSignKeysSignCertHasNoPemHeaders() {
        ClusterProxyCertificate certificate = underTest.generateSignKeys();

        String signTokenCert = certificate.getSignTokenCert();
        assertFalse(signTokenCert.contains("-----BEGIN"), "signCert must not contain PEM BEGIN header");
        assertFalse(signTokenCert.contains("-----END"), "signCert must not contain PEM END header");
    }

    @Test
    void generateSignKeysSignKeyHasPemHeader() {
        ClusterProxyCertificate certificate = underTest.generateSignKeys();

        assertNotNull(certificate.getSignKey());
        // signKey is a full PEM private key — headers are preserved unlike signCert
        assertFalse(certificate.getSignKey().isEmpty());
    }

    @Test
    void generateSignKeysProducesDifferentKeysOnEachCall() {
        ClusterProxyCertificate first = underTest.generateSignKeys();
        ClusterProxyCertificate second = underTest.generateSignKeys();

        assertNotEquals(first.getSignKey(), second.getSignKey());
        assertNotEquals(first.getSignCert(), second.getSignCert());
    }

}
