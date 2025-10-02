package com.sequenceiq.cloudbreak.certificate;

import static com.sequenceiq.cloudbreak.certificate.PkiUtil.SALT_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.util.encoders.DecoderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.io.BaseEncoding;

class PkiUtilTest {

    // cbd-local/certs/traefik/client-ca.pem
    private static final String CERT_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIICzTCCAbWgAwIBAgIQQF+qdC5OLSiBa6GQOYODbzANBgkqhkiG9w0BAQsFADAQ\n" +
                    "MQ4wDAYDVQQKEwVsb2NhbDAeFw0xOTA0MTExMTE1MDBaFw0yMjAzMjYxMTE1MDBa\n" +
                    "MBAxDjAMBgNVBAoTBWxvY2FsMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\n" +
                    "AQEAyAW4ZuDjMSA7QrD/uyWon9knRbyrdWAu1t5ywwRbJ1umemrM8zxx8ZCttLj5\n" +
                    "EwsGOpif6emVNoea9UhHhC/WePqE5HV7mjM/be1OlJJAVcnVgMchM+XFeoRujkUa\n" +
                    "6MdQUR5y+cp4UvcSxncab+k8yzCl30hjN7SQGwnBWfsJSIEUKN2NkbOxNeeSVyNI\n" +
                    "MR98Av7HojC1KB6CtNV7CUTlc1Lu9iQUzv/dNEWSjsuHNbowL2h8JGE8VffjfoxO\n" +
                    "S3qerrPfaXtq01e4kOjyTIuFcdXj5A6gLHuHcYhkbrE+FElfOhU8NKqQ8n3SgBcd\n" +
                    "x2lp/OrWE8NWmuTLTMOTRqQkhwIDAQABoyMwITAOBgNVHQ8BAf8EBAMCAqwwDwYD\n" +
                    "VR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAl0jUCbxNxrLee+/c8Wjo\n" +
                    "ku6W/5DDD3YgQA5DAiXj4+MyQ2QsYwigqW5dv6fL1iY2RqNrvd6ZWiGfITvKuR4v\n" +
                    "Fzfn1FsxRFINBgZo0GE70lmRYM973qFrmX1Bc1DPL8Q4zIFbOtBPMgb+8jEC9Cut\n" +
                    "inyGIk8VnjS7Dt0x1iPDYfVLiaHHuMxH+einQfeVQOq1oDE/VCWTW420SH+7+76u\n" +
                    "7HjTgxERz1KFPH00+DM9IRuVZg4xKrsxfzjALKHj95GqCFU6uwz3m1em8zPAD9sB\n" +
                    "IHkVa8ryxFK8uVxRnpdoPxfivpCmxo9QCble6SwIuk6FqMCLdxzhsxt5TMtoWkqn\n" +
                    "WQ==\n" +
                    "-----END CERTIFICATE-----";

    private static final String CERT_ISSUER = "O=local";

    private static final String MALFORMED_CERTIFICATE_IO_EXCEPTION = "-----BEGIN CERTIFICATE-----\n";

    private static final String MALFORMED_CERTIFICATE_IO_EXCEPTION_2 =
            "-----BEGIN CERTIFICATE-----\n" +
                    "foobar==\n" +
                    "-----END CERTIFICATE-----";

    private static final String MALFORMED_CERTIFICATE_DECODER_EXCEPTION =
            "-----BEGIN CERTIFICATE-----\n" +
                    "foo\n" +
                    "-----END CERTIFICATE-----";

    @Test
    void certTest() {
        KeyPair identityKey = PkiUtil.generateKeypair();
        KeyPair signKey = PkiUtil.generateKeypair();

        X509Certificate cert = PkiUtil.cert(identityKey, "192.168.99.100", signKey);

        String pk = PkiUtil.convert(identityKey.getPrivate());
        String cer = PkiUtil.convert(cert);

        assertThat(pk).isNotNull();
        assertThat(cer).isNotNull();
    }

    @Test
    void fromPrivateKeyPemTest() {
        KeyPair keyPair = PkiUtil.generateKeypair();

        KeyPair actual = PkiUtil.fromPrivateKeyPem(PkiUtil.convert(keyPair.getPrivate()));

        assertThat(actual).isNotNull();
        assertThat(actual.getPrivate()).isEqualTo(keyPair.getPrivate());
        assertThat(actual.getPublic()).isEqualTo(keyPair.getPublic());
    }

    @Test
    void fromCertificatePemTestWhenNull() {
        verifyPkiException(() -> PkiUtil.fromCertificatePem(null), NullPointerException.class);
    }

    private void verifyPkiException(Executable executable, Class<? extends Throwable> type) {
        PkiException pkiException = assertThrows(PkiException.class, executable);

        assertThat(pkiException).hasMessage("Cannot parse X.509 certificate from PEM content");
        assertThat(pkiException).hasCauseInstanceOf(type);
    }

    @Test
    void fromCertificatePemTestWhenMalformedIOException() {
        verifyPkiException(() -> PkiUtil.fromCertificatePem(MALFORMED_CERTIFICATE_IO_EXCEPTION), IOException.class);
    }

    @Test
    void fromCertificatePemTestWhenMalformedIOException2() {
        verifyPkiException(() -> PkiUtil.fromCertificatePem(MALFORMED_CERTIFICATE_IO_EXCEPTION_2), IOException.class);
    }

    @Test
    void fromCertificatePemTestWhenMalformedDecoderException() {
        verifyPkiException(() -> PkiUtil.fromCertificatePem(MALFORMED_CERTIFICATE_DECODER_EXCEPTION), DecoderException.class);
    }

    @Test
    void fromCertificatePemTestWhenEmpty() {
        PkiException pkiException = assertThrows(PkiException.class, () -> PkiUtil.fromCertificatePem(""));

        assertThat(pkiException)
                .hasMessage("Cannot parse X.509 certificate from PEM content. Expected \"org.bouncycastle.cert.X509CertificateHolder\" object, got \"null\".");
    }

    @Test
    void fromCertificatePemTestWhenNotCertContent() {
        KeyPair keyPair = PkiUtil.generateKeypair();

        PkiException pkiException = assertThrows(PkiException.class, () -> PkiUtil.fromCertificatePem(PkiUtil.convert(keyPair.getPrivate())));

        assertThat(pkiException)
                .hasMessage("Cannot parse X.509 certificate from PEM content. Expected \"org.bouncycastle.cert.X509CertificateHolder\" object, " +
                        "got \"org.bouncycastle.openssl.PEMKeyPair\".");
    }

    @Test
    void fromCertificatePemTestWhenSuccess() {
        X509Certificate x509Certificate = PkiUtil.fromCertificatePem(CERT_PEM);

        assertThat(x509Certificate).isNotNull();

        X500Principal issuerX500Principal = x509Certificate.getIssuerX500Principal();
        assertThat(issuerX500Principal).isNotNull();
        assertThat(issuerX500Principal.getName()).isEqualTo(CERT_ISSUER);
    }

    @Test
    void testGenerateSignatureBCFIPSProvider() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException {
        Security.addProvider(new BouncyCastleFipsProvider());

        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKeyAsString = PkiUtil.convert(keyPair.getPrivate());
        String data = "somedata";

        String signedData = PkiUtil.generateSignature(privateKeyAsString, data.getBytes());

        Signature instance = Signature.getInstance("SHA256withRSAandMGF1");
        instance.initVerify(keyPair.getPublic());
        instance.setParameter(new PSSParameterSpec("SHA-256", "MGF1",
                new MGF1ParameterSpec("SHA-256"), SALT_LENGTH, PSSParameterSpec.DEFAULT.getTrailerField()));
        instance.update(data.getBytes());
        assertTrue(instance.verify(BaseEncoding.base64().decode(signedData)));
    }

    @Test
    void testGenerateSignatureBCProvider() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException {
        Security.addProvider(new BouncyCastleProvider());

        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKeyAsString = PkiUtil.convert(keyPair.getPrivate());
        String data = "somedata";

        String signedData = PkiUtil.generateSignature(privateKeyAsString, data.getBytes());

        Signature instance = Signature.getInstance("SHA256withRSAandMGF1");
        instance.initVerify(keyPair.getPublic());
        instance.setParameter(new PSSParameterSpec("SHA-256", "MGF1",
                new MGF1ParameterSpec("SHA-256"), SALT_LENGTH, PSSParameterSpec.DEFAULT.getTrailerField()));
        instance.update(data.getBytes());
        assertTrue(instance.verify(BaseEncoding.base64().decode(signedData)));
    }

    @Test
    void testEcdsaGeneration() {
        Security.addProvider(new BouncyCastleFipsProvider());
        KeyPair keyPair = PkiUtil.generateEcdsaKeypair();
        String privateKey = PkiUtil.convertEcPrivateKey(keyPair.getPrivate());

        assertThat(privateKey.trim()).startsWith("-----BEGIN EC PRIVATE KEY-----");
        assertThat(privateKey.trim()).hasSizeGreaterThan(200);
        assertThat(privateKey.trim()).endsWith("-----END EC PRIVATE KEY-----");
    }

    @Test
    void testNullEcPrivateKey() {
        assertThrows(PkiException.class, () -> PkiUtil.convertEcPrivateKey(null));
    }

    @Test
    void testEcPrivateKey222() {
        PkiException ex = assertThrows(PkiException.class, PkiUtil::generateEcdsaKeypair);
        assertEquals("Failed to generate ECDSA PK for the cluster!", ex.getMessage());
    }

    @Test
    void testGetSignatureAlgorithmByPrivateKey() {
        PrivateKey privateKey = mock(PrivateKey.class);

        when(privateKey.getAlgorithm()).thenReturn("EC");
        assertEquals("SHA384withECDSA", PkiUtil.getSignatureAlgorithmByPrivateKey(privateKey));

        when(privateKey.getAlgorithm()).thenReturn("ECDSA");
        assertEquals("SHA384withECDSA", PkiUtil.getSignatureAlgorithmByPrivateKey(privateKey));

        when(privateKey.getAlgorithm()).thenReturn("RSA");
        assertEquals("SHA256withRSA", PkiUtil.getSignatureAlgorithmByPrivateKey(privateKey));
    }
}
