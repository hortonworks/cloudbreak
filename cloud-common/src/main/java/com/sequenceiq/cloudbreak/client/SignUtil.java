package com.sequenceiq.cloudbreak.client;

import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import com.google.common.io.BaseEncoding;

public class SignUtil {

    private SignUtil() {
    }

    public static String generateSignature(String privateKeyContent, byte[] content) {
        try (PEMParser pEMParser = new PEMParser(new StringReader(privateKeyContent))) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) pEMParser.readObject();
            byte[] pemPrivateKeyEncoded = pemKeyPair.getPrivateKeyInfo().getEncoded();

            KeyFactory factory = KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemPrivateKeyEncoded);
            PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

            Signature signer = Signature.getInstance("SHA256WithRSA");
            signer.initSign(privateKey);

            signer.update(content);

            return BaseEncoding.base64().encode(signer.sign());
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            throw new SecurityException(e);
        }
    }
}
