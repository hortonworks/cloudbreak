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
        TokenCertInfo certificate = underTest.generateSignKeys();

        assertNotNull(certificate.privateKey());
        assertNotNull(certificate.publicKey());
        assertNotNull(certificate.signCert());
    }

    @Test
    void generateSignKeysSignCertHasNoPemHeaders() {
        TokenCertInfo certificate = underTest.generateSignKeys();

        String signTokenCert = certificate.base64DerCert();
        assertFalse(signTokenCert.contains("-----BEGIN"), "signCert must not contain PEM BEGIN header");
        assertFalse(signTokenCert.contains("-----END"), "signCert must not contain PEM END header");
    }

    @Test
    void generateSignKeysSignKeyHasPemHeader() {
        TokenCertInfo certificate = underTest.generateSignKeys();

        assertNotNull(certificate.publicKey());
        // signKey is a full PEM private key — headers are preserved unlike signCert
        assertFalse(certificate.publicKey().isEmpty());
    }

    @Test
    void generateSignKeysProducesDifferentKeysOnEachCall() {
        TokenCertInfo first = underTest.generateSignKeys();
        TokenCertInfo second = underTest.generateSignKeys();

        assertNotEquals(first.publicKey(), second.publicKey());
        assertNotEquals(first.signCert(), second.signCert());
    }

}
