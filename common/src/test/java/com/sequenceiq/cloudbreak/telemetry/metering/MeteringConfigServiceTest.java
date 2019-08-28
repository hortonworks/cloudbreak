package com.sequenceiq.cloudbreak.telemetry.metering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class MeteringConfigServiceTest {

    private MeteringConfigService underTest;

    @Before
    public void setUp() {
        underTest = new MeteringConfigService();
    }

    @Test
    public void testCreateMeteringConfigs() {
        // GIVEN
        // WHEN
        MeteringConfigView result = underTest.createMeteringConfigs(true, "AWS", "myCrn",
                "DATAHUB", "1.0.0");
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("DATAHUB", result.getServiceType());
    }

    @Test
    public void testCreateMeteringConfigsWithLowercaseServiceType() {
        // GIVEN
        // WHEN
        MeteringConfigView result = underTest.createMeteringConfigs(true, "AWS", "myCrn",
                "datahub", "1.0.0");
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("DATAHUB", result.getServiceType());
    }

    @Test
    public void testCreateMeteringConfigsIfDisabled() {
        // GIVEN
        // WHEN
        MeteringConfigView result = underTest.createMeteringConfigs(false, "AWS", "myCrn",
                "DATAHUB", "1.0.0");
        // THEN
        assertFalse(result.isEnabled());
    }
}
