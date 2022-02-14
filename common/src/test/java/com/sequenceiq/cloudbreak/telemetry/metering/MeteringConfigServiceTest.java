package com.sequenceiq.cloudbreak.telemetry.metering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;

public class MeteringConfigServiceTest {

    private MeteringConfigService underTest;

    @Before
    public void setUp() {
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "app", "stream");
        TelemetryComponentUpgradeConfiguration meteringAgentConfig = new TelemetryComponentUpgradeConfiguration();
        meteringAgentConfig.setDesiredDate("2021-01-01");
        TelemetryUpgradeConfiguration upgradeConfigs = new TelemetryUpgradeConfiguration();
        upgradeConfigs.setEnabled(true);
        upgradeConfigs.setMeteringAgent(meteringAgentConfig);
        underTest = new MeteringConfigService(meteringConfiguration, upgradeConfigs);
    }

    @Test
    public void testCreateMeteringConfigs() {
        // GIVEN
        // WHEN
        MeteringConfigView result = underTest.createMeteringConfigs(true, "AWS", "myName", "myCrn",
                "DATAHUB", "1.0.0");
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("DATAHUB", result.getServiceType());
        assertEquals("stream", result.getStreamName());
    }

    @Test
    public void testCreateMeteringConfigsWithLowercaseServiceType() {
        // GIVEN
        // WHEN
        MeteringConfigView result = underTest.createMeteringConfigs(true, "AWS", "myName", "myCrn",
                "datahub", "1.0.0");
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("DATAHUB", result.getServiceType());
    }

    @Test
    public void testCreateMeteringConfigsIfDisabled() {
        // GIVEN
        // WHEN
        MeteringConfigView result = underTest.createMeteringConfigs(false, "AWS", "myName", "myCrn",
                "DATAHUB", "1.0.0");
        // THEN
        assertFalse(result.isEnabled());
    }
}
