package com.sequenceiq.cloudbreak.client;

import static net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.ED_25519_CURVE_SPEC;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

public class Ed25519HeaderSigner extends HeaderSigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ed25519HeaderSigner.class);

    @Override
    protected String urlsafeSignature(String seedBase64, String contentType, String method, String path, String date) {
        byte[] seed = Base64Util.decodeAsByteArray(seedBase64);
        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(seed, ED_25519_CURVE_SPEC);
        PrivateKey privateKey = new EdDSAPrivateKey(privKeySpec);
        try {
            Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
            sgr.initSign(privateKey);
            String messageToSign = method + "\n" + contentType + "\n" + date + "\n" + path + "\n" + AUTH_METHOD_ED25519;
            sgr.update(messageToSign.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getUrlEncoder().encode(sgr.sign()), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can not find SHA-512", e);
            throw new IllegalStateException("Can not find SHA-512", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Invalid private key for signing", e);
            throw new IllegalArgumentException("Invalid private key for signing", e);
        } catch (SignatureException e) {
            LOGGER.error("Signing failed", e);
            throw new IllegalArgumentException("Signing failed", e);
        }
    }

    @Override
    protected String getAuthMethod() {
        return AUTH_METHOD_ED25519;
    }
}
