package com.sequenceiq.cloudbreak.client;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import com.google.common.io.BaseEncoding;

public class RsaKeyUtil {

    private static final Integer SALT_LENGTH = 20;

    private RsaKeyUtil() {
    }

    public static byte[] getPublicKeyDer(String privateKeyPem) {
        try (PEMParser pemParser = new PEMParser(new StringReader(clarifyPemKey(privateKeyPem)))) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
            return pemKeyPair.getPublicKeyInfo().getEncoded();
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    public static String generateSignature(String privateKeyPem, byte[] data) {
        try (PEMParser pEMParser = new PEMParser(new StringReader(clarifyPemKey(privateKeyPem)))) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) pEMParser.readObject();

            KeyFactory factory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pemKeyPair.getPublicKeyInfo().getEncoded());
            PublicKey publicKey = factory.generatePublic(publicKeySpec);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemKeyPair.getPrivateKeyInfo().getEncoded());
            PrivateKey privateKey = factory.generatePrivate(privateKeySpec);
            KeyPair kp = new KeyPair(publicKey, privateKey);
            RSAPrivateKeySpec privKeySpec = factory.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);

            PSSSigner signer = new PSSSigner(new RSAEngine(), new SHA256Digest(), SALT_LENGTH);
            signer.init(true, new RSAKeyParameters(true, privKeySpec.getModulus(), privKeySpec.getPrivateExponent()));
            signer.update(data, 0, data.length);
            byte[] signature = signer.generateSignature();

            return BaseEncoding.base64().encode(signature);
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException | CryptoException e) {
            throw new SecurityException(e);
        }
    }

    private static String clarifyPemKey(String rawPem) {
        return "-----BEGIN RSA PRIVATE KEY-----\n" + rawPem.replaceAll("-----(.*)-----|\n", "") + "\n-----END RSA PRIVATE KEY-----";
    }
}
