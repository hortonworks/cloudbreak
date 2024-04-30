package com.sequenceiq.cloudbreak.service.secret.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class VaultSecretConverterTest {

    private VaultSecretConverter underTest = new VaultSecretConverter();

    @Test
    void testConvert() {
        VaultSecret result = underTest.convert("{\"enginePath\":\"enginePath\",\"engineClass\":\"engineClass\",\"path\":\"path\"}");

        assertEquals("enginePath", result.getEnginePath());
        assertEquals("engineClass", result.getEngineClass());
        assertEquals("path", result.getPath());
    }

    @Test
    void testInvalidJsonReturnsNull() {
        VaultSecret result = underTest.convert("this is not a json");

        assertNull(result);
    }

    @Test
    void testMissingValues() {
        // engineClass, enginePath and path are mandatory
        VaultSecret result = underTest.convert("{\"engineClass\":\"engineClass\",\"path\":\"path\"}");

        assertNull(result);
    }

}