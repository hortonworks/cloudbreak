package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;

class SaltSecurityConfigTest {

    private static final String PRIVATE_KEY = PkiUtil.generatePemPrivateKeyInBase64();

    @Test
    void testGetSaltBootSignPublicKeyWhenPrivateKeyExists() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        String expectedPublicKey = PkiUtil.calculatePemPublicKeyInBase64(PRIVATE_KEY);
        saltSecurityConfig.setSaltBootSignPrivateKey(PRIVATE_KEY);

        String result = saltSecurityConfig.getSaltBootSignPublicKey();

        assertEquals(expectedPublicKey, result);
    }

    @Test
    void testGetSaltBootSignPublicKeyWhenPrivateKeyIsNull() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltBootSignPrivateKey(null);

        String result = saltSecurityConfig.getSaltBootSignPublicKey();

        assertNull(result);
    }

    @Test
    void testGetSaltSignPublicKeyWhenPrivateKeyExists() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        String expectedPublicKey = PkiUtil.calculatePemPublicKeyInBase64(PRIVATE_KEY);
        saltSecurityConfig.setSaltSignPrivateKey(PRIVATE_KEY);

        String result = saltSecurityConfig.getSaltSignPublicKey();

        assertEquals(expectedPublicKey, result);
    }

    @Test
    void testGetSaltSignPublicKeyWhenPrivateKeyIsNull() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltSignPrivateKey(null);

        String result = saltSecurityConfig.getSaltSignPublicKey();

        assertNull(result);
    }

    @Test
    void testGetSaltMasterPublicKeyWhenPrivateKeyExists() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        String expectedPublicKey = PkiUtil.calculatePemPublicKeyInBase64(PRIVATE_KEY);
        saltSecurityConfig.setSaltMasterPrivateKey(PRIVATE_KEY);

        String result = saltSecurityConfig.getSaltMasterPublicKey();

        assertEquals(expectedPublicKey, result);
    }

    @Test
    void testGetSaltMasterPublicKeyWhenPrivateKeyIsNull() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltMasterPrivateKey(null);

        String result = saltSecurityConfig.getSaltMasterPublicKey();

        assertNull(result);
    }

}