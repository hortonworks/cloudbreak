package com.sequenceiq.freeipa.entity;

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
        saltSecurityConfig.setSaltBootSignPrivateKeyVault(PRIVATE_KEY);

        String result = saltSecurityConfig.getSaltBootSignPublicKey();

        assertEquals(expectedPublicKey, result);
    }

    @Test
    void testGetSaltBootSignPublicKeyWhenPrivateKeyIsNull() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltBootSignPrivateKeyVault((String) null);

        String result = saltSecurityConfig.getSaltBootSignPublicKey();

        assertNull(result);
    }

    @Test
    void testGetSaltSignPublicKeyWhenPrivateKeyExists() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        String expectedPublicKey = PkiUtil.calculatePemPublicKeyInBase64(PRIVATE_KEY);
        saltSecurityConfig.setSaltSignPrivateKeyVault(PRIVATE_KEY);

        String result = saltSecurityConfig.getSaltSignPublicKey();

        assertEquals(expectedPublicKey, result);
    }

    @Test
    void testGetSaltSignPublicKeyWhenPrivateKeyIsNull() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltSignPrivateKeyVault((String) null);

        String result = saltSecurityConfig.getSaltSignPublicKey();

        assertNull(result);
    }

    @Test
    void testGetSaltMasterPublicKeyWhenPrivateKeyExists() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        String expectedPublicKey = PkiUtil.calculatePemPublicKeyInBase64(PRIVATE_KEY);
        saltSecurityConfig.setSaltMasterPrivateKeyVault(PRIVATE_KEY);

        String result = saltSecurityConfig.getSaltMasterPublicKey();

        assertEquals(expectedPublicKey, result);
    }

    @Test
    void testGetSaltMasterPublicKeyWhenPrivateKeyIsNull() {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltMasterPrivateKeyVault((String) null);

        String result = saltSecurityConfig.getSaltMasterPublicKey();

        assertNull(result);
    }
}