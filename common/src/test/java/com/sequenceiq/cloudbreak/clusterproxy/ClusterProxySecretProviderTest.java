package com.sequenceiq.cloudbreak.clusterproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
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

    @Test
    void generateSignKeysPrivateKeyMatchesCertificatePublicKey() throws Exception {
        TokenCertInfo tokenCertInfo = underTest.generateSignKeys();

        // Parse private key from PEM
        PemReader pemReader = new PemReader(new StringReader(tokenCertInfo.privateKey()));
        PemObject pemObject = pemReader.readPemObject();
        pemReader.close();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        // Parse certificate from PEM
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new java.io.ByteArrayInputStream(tokenCertInfo.signCert().getBytes()));
        PublicKey certPublicKey = cert.getPublicKey();

        // The private key's modulus must match the certificate's public key modulus
        RSAPublicKey rsaPublicKey = (RSAPublicKey) certPublicKey;
        assertEquals(privateKey.getModulus(), rsaPublicKey.getModulus(),
                "Private key modulus must match certificate public key modulus — " +
                        "openssl pkcs12 will reject mismatched signkey.pem + signcert.pem");
    }

}
