package com.sequenceiq.cloudbreak.telemetry.databus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DatabusConfigServiceTest {

    private DatabusConfigService underTest;

    @Before
    public void setUp() {
        underTest = new DatabusConfigService();
    }

    @Test
    public void testCreateConfig() {
        // GIVEN
        // WHEN
        DatabusConfigView result = underTest.createDatabusConfigs("accessKey", "secret".toCharArray(), null, "myEndpoint");
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("accessKey", result.getAccessKeyId());
        assertEquals("secret", new String(result.getAccessKeySecret()));
        assertEquals("myEndpoint", result.getEndpoint());
        assertEquals("Ed25519", result.toMap().get("accessKeySecretAlgorithm"));
    }

    @Test
    public void testCreateConfigWithAlgorithm() {
        // GIVEN
        // WHEN
        DatabusConfigView result = underTest.createDatabusConfigs("accessKey", "secret".toCharArray(), "RSA", "myEndpoint");
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("accessKey", result.getAccessKeyId());
        assertEquals("secret", new String(result.getAccessKeySecret()));
        assertEquals("myEndpoint", result.getEndpoint());
        assertEquals("RSA", result.toMap().get("accessKeySecretAlgorithm"));
    }

    @Test
    public void testCreateConfigWithoutSecret() {
        // GIVEN
        // WHEN
        DatabusConfigView result = underTest.createDatabusConfigs("accessKey", null, null, "myEndpoint");
        // THEN
        assertFalse(result.isEnabled());
    }

    @Test
    public void testCreateConfigWithoutEndpoint() {
        // GIVEN
        // WHEN
        DatabusConfigView result = underTest.createDatabusConfigs("accessKey", "secret".toCharArray(), null, null);
        // THEN
        assertFalse(result.isEnabled());
    }
}
