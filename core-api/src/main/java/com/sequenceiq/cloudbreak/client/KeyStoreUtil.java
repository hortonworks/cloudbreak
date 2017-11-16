package com.sequenceiq.cloudbreak.client;

import java.io.IOException;
import java.io.Reader;
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

public class KeyStoreUtil {

    private KeyStoreUtil() throws IllegalAccessException {
        throw new IllegalAccessException("KeyStoreUtil could not be initialized");
    }

    public static KeyStore createKeyStore(String clientCert, String clientKey) throws Exception {
        KeyPair keyPair = createKeyPair(clientKey);
        Certificate privateCertificate = convertCertificate(clientCert);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);

        Certificate[] cert = {privateCertificate};
        keyStore.setKeyEntry("client", keyPair.getPrivate(), "consul".toCharArray(), cert);
        return keyStore;
    }

    public static KeyStore createTrustStore(String serverCert) throws Exception {
        Reader reader = null;
        PEMParser pemParser = null;

        try {
            reader = new StringReader(serverCert);
            pemParser = new PEMParser(reader);
            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
            Certificate caCertificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null);
            trustStore.setCertificateEntry("ca", caCertificate);
            return trustStore;

        } finally {
            if (pemParser != null) {
                pemParser.close();
            }

            if (reader != null) {
                reader.close();
            }
        }
    }

    private static Certificate convertCertificate(String cert) throws IOException, CertificateException {
        Reader reader = null;
        PEMParser pemParser = null;

        try {
            reader = new StringReader(cert);
            pemParser = new PEMParser(reader);
            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
            return new JcaX509CertificateConverter().getCertificate(certificateHolder);
        } finally {
            if (pemParser != null) {
                pemParser.close();
            }

            if (reader != null) {
                reader.close();
            }
        }

    }

    public static KeyPair createKeyPair(String clientKey) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        Reader reader = null;
        PEMParser pemParser = null;

        try {
            reader = new StringReader(clientKey);
            pemParser = new PEMParser(reader);

            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();

            byte[] pemPrivateKeyEncoded = pemKeyPair.getPrivateKeyInfo().getEncoded();
            byte[] pemPublicKeyEncoded = pemKeyPair.getPublicKeyInfo().getEncoded();

            KeyFactory factory = KeyFactory.getInstance("RSA");

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pemPublicKeyEncoded);
            PublicKey publicKey = factory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemPrivateKeyEncoded);
            PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);

        } finally {
            if (pemParser != null) {
                pemParser.close();
            }

            if (reader != null) {
                reader.close();
            }
        }

    }
}
