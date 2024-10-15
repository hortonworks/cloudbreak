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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;

public class KeyStoreUtil {

    private KeyStoreUtil() throws IllegalAccessException {
        throw new IllegalAccessException("KeyStoreUtil could not be initialized");
    }

    public static KeyStore createKeyStore(String clientCert, String clientKey) throws Exception {
        KeyPair keyPair = createKeyPair(clientKey);
        Certificate privateCertificate = PkiUtil.fromCertificatePem(clientCert);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);

        Certificate[] cert = {privateCertificate};
        keyStore.setKeyEntry("client", keyPair.getPrivate(), "consul".toCharArray(), cert);
        return keyStore;
    }

    public static KeyStore createTrustStore(String serverCert, Optional<String> additionalServerCert) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null);
        try (Reader reader1 = new StringReader(serverCert)) {
            try (PEMParser pemParser1 = new PEMParser(reader1)) {
                X509CertificateHolder certificateHolder1 = (X509CertificateHolder) pemParser1.readObject();
                Certificate caCertificate1 = new JcaX509CertificateConverter().getCertificate(certificateHolder1);
                trustStore.setCertificateEntry("ca", caCertificate1);
            }
        }
        if (additionalServerCert.isPresent()) {
            try (Reader reader2 = new StringReader(additionalServerCert.get())) {
                try (PEMParser pemParser2 = new PEMParser(reader2)) {
                    X509CertificateHolder certificateHolder2 = (X509CertificateHolder) pemParser2.readObject();
                    Certificate caCertificate2 = new JcaX509CertificateConverter().getCertificate(certificateHolder2);
                    trustStore.setCertificateEntry("newca", caCertificate2);
                }
            }
        }
        return trustStore;
    }

    public static KeyPair createKeyPair(String clientKey) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        try (Reader reader = new StringReader(clientKey)) {
            try (PEMParser pemParser = new PEMParser(reader)) {

                PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();

                byte[] pemPrivateKeyEncoded = pemKeyPair.getPrivateKeyInfo().getEncoded();
                byte[] pemPublicKeyEncoded = pemKeyPair.getPublicKeyInfo().getEncoded();

                KeyFactory factory = KeyFactory.getInstance("RSA");

                KeySpec publicKeySpec = new X509EncodedKeySpec(pemPublicKeyEncoded);
                PublicKey publicKey = factory.generatePublic(publicKeySpec);

                KeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemPrivateKeyEncoded);
                PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

                return new KeyPair(publicKey, privateKey);
            }
        }
    }

}
