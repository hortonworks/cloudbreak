package com.sequenceiq.cloudbreak.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;

public class Ed25519HeaderSigner extends HeaderSigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ed25519HeaderSigner.class);

    @Override
    protected String urlsafeSignature(String seedBase64, String contentType, String method, String path, String date) {
        try {
            // Decode the 32-byte Ed25519 seed
            byte[] seed = Base64Util.decodeAsByteArray(seedBase64);

            // Construct PKCS#8 structure for Ed25519 private key from seed
            AlgorithmIdentifier algId = new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519);
            // Ed25519 PKCS#8 expects an ASN.1 Octet String containing the seed
            PrivateKeyInfo pkInfo = new PrivateKeyInfo(algId, new DEROctetString(seed));
            byte[] pkcs8Bytes = pkInfo.getEncoded();

            // Generate PrivateKey object from PKCS#8
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BCFIPS");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));

            // Prepare the message to sign
            String messageToSign = method + "\n" + contentType + "\n" + date + "\n" + path + "\n" + AUTH_METHOD_ED25519;
            byte[] messageBytes = messageToSign.getBytes(StandardCharsets.UTF_8);

            // Sign the message
            Signature signature = Signature.getInstance("Ed25519", "BCFIPS");
            signature.initSign(privateKey);
            signature.update(messageBytes);
            byte[] signatureBytes = signature.sign();

            // Return URL-safe Base64 encoded signature
            return Base64Util.encodeToUrlString(signatureBytes);

        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can not find SHA-512", e);
            throw new IllegalStateException("Can not find SHA-512", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Invalid private key for signing", e);
            throw new IllegalArgumentException("Invalid private key for signing", e);
        } catch (SignatureException e) {
            LOGGER.error("Signing failed", e);
            throw new IllegalArgumentException("Signing failed", e);
        } catch (NoSuchProviderException e) {
            LOGGER.error("Failed to get security provider", e);
            throw new IllegalArgumentException("Failed to get security provider", e);
        } catch (IOException e) {
            LOGGER.error("Failed to decode private key", e);
            throw new IllegalArgumentException("Failed to decode private key", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Failed to generate private key", e);
            throw new IllegalArgumentException("Failed to generate private key", e);
        }
    }

    @Override
    protected String getAuthMethod() {
        return AUTH_METHOD_ED25519;
    }
}
