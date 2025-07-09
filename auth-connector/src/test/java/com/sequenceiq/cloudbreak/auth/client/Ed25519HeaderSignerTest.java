package com.sequenceiq.cloudbreak.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.Security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.client.Ed25519HeaderSigner;

class Ed25519HeaderSignerTest {

    private Ed25519HeaderSignerImpl signer;

    @BeforeEach
    void setUp() {
        signer = new Ed25519HeaderSignerImpl();
    }

    @Test
    void testUrlsafeSignatureReturnsNonEmptyString() {
        Security.addProvider(new BouncyCastleFipsProvider());

        String seedBase64 = "n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg=";
        String contentType = "application/json";
        String method = "POST";
        String path = "/api/test";
        String date = "2025-07-09T22:38:00Z";

        String signature = signer.urlsafeSignature(seedBase64, contentType, method, path, date);

        assertNotNull(signature, "Signature should not be null");
        assertFalse(signature.isEmpty(), "Signature should not be empty");
        assertEquals("ESeG_C5o2hQ44KcoYZKJjXYydTbCEZuV5w12paeas7tt8N7vj3rsgSSZ26VfC_RSPp7zrIS4XGBcRQxds36nCg==", signature);
    }

    public class Ed25519HeaderSignerImpl extends Ed25519HeaderSigner {
        public String urlsafeSignature(String seedBase64, String contentType, String method, String path, String date) {
            return super.urlsafeSignature(seedBase64, contentType, method, path, date);
        }
    }
}