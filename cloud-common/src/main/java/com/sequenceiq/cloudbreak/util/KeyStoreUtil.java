package com.sequenceiq.cloudbreak.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public static KeyStore createKeyStore(final String clientCertPath, String clientKeyPath) throws Exception {
        KeyPair keyPair = loadPrivateKey(clientKeyPath);
        Certificate privateCertificate = loadCertificate(clientCertPath);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);

        Certificate[] cert = new Certificate[]{privateCertificate};
        keyStore.setKeyEntry("client", keyPair.getPrivate(), "consul".toCharArray(), cert);
        return keyStore;
    }

    public static KeyStore createTrustStore(final String serverCertPath) throws Exception {
        File serverCertFile = new File(serverCertPath);
        BufferedReader reader = new BufferedReader(new FileReader(serverCertFile));
        PEMParser pemParser = null;

        try {
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
                pemParser.close();
            }
        }
    }

    private static Certificate loadCertificate(final String certPath) throws IOException, CertificateException {
        File certificate = new File(certPath);
        BufferedReader reader = new BufferedReader(new FileReader(certificate));
        PEMParser pemParser = null;

        try {
            pemParser = new PEMParser(reader);
            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
            return new JcaX509CertificateConverter().getCertificate(certificateHolder);
        } finally {
            if (pemParser != null) {
                pemParser.close();
            }

            if (reader != null) {
                pemParser.close();
            }
        }

    }

    private static KeyPair loadPrivateKey(final String clientKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File privateKeyFile = new File(clientKeyPath);
        BufferedReader reader = new BufferedReader(new FileReader(privateKeyFile));

        PEMParser pemParser = null;

        try {
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
                pemParser.close();
            }
        }

    }
}
