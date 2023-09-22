package com.sequenceiq.cloudbreak.client;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;

public class EcdsaHeaderSigner extends HeaderSigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EcdsaHeaderSigner.class);

    @Override
    protected String urlsafeSignature(String privateKey, String contentType, String method, String path, String date) {
        try {
            PrivateKey key = readPrivateKeyFromString(privateKey);
            Signature dsa = Signature.getInstance("SHA512withECDSA");
            dsa.initSign(key);
            String messageToSign = method + "\n" + contentType + "\n" + date + "\n" + path + "\n" + AUTH_METHOD_ECDSA;
            dsa.update(messageToSign.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getUrlEncoder().encode(dsa.sign()), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can not find algorithms SHA512withECDSA or EC.", e);
            throw new IllegalStateException("Can not find algorithms SHA512withECDSA or EC.", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("The specification is invalid for EC.", e);
            throw new IllegalStateException("The specification is invalid for EC.", e);
        } catch (SignatureException e) {
            LOGGER.error("Signing failed", e);
            throw new IllegalArgumentException("Signing failed", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Invalid private key for signing", e);
            throw new IllegalArgumentException("Invalid private key for signing", e);
        }
    }

    @Override
    protected String getAuthMethod() {
        return AUTH_METHOD_ECDSA;
    }

    private PrivateKey readPrivateKeyFromString(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyPEM = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64Util.decodeAsByteArray(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (ECPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
