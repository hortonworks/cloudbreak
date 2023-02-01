package com.sequenceiq.freeipa.client;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

/**
 * Utility class to create a Keystore used for 2-way-ssl connection.
 */
public class KeystoreUtils {

    private KeystoreUtils() {
    }

    public static KeyStore createKeyStore(final String clientCert, String clientKey) throws Exception {
        KeyPair keyPair = loadPrivateKey(clientKey);
        Certificate privateCertificate = loadCertificate(clientCert);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);

        Certificate[] cert = new Certificate[]{privateCertificate};
        keyStore.setKeyEntry("client", keyPair.getPrivate(), "consul".toCharArray(), cert);
        return keyStore;
    }

    public static KeyStore createTrustStore(final String serverCert) throws Exception {
        StringReader reader = new StringReader(serverCert);
        try (PEMParser pemParser = new PEMParser(reader)) {
            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
            Certificate caCertificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null);
            trustStore.setCertificateEntry("ca", caCertificate);
            return trustStore;
        }
    }

    private static Certificate loadCertificate(final String cert) throws IOException, CertificateException {
        StringReader reader = new StringReader(cert);

        try (PEMParser pemParser = new PEMParser(reader)) {
            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
            return new JcaX509CertificateConverter().getCertificate(certificateHolder);
        }
    }

    private static KeyPair loadPrivateKey(final String clientKey) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        StringReader reader = new StringReader(clientKey);

        try (PEMParser pemParser = new PEMParser(reader)) {

            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();

            byte[] pemPrivateKeyEncoded = pemKeyPair.getPrivateKeyInfo().getEncoded();
            byte[] pemPublicKeyEncoded = pemKeyPair.getPublicKeyInfo().getEncoded();

            KeyFactory factory = KeyFactory.getInstance("RSA");

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pemPublicKeyEncoded);
            PublicKey publicKey = factory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemPrivateKeyEncoded);
            PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        }
    }
}
