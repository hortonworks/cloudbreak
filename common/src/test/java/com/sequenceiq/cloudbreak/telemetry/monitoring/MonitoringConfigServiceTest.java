package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;

public class MonitoringConfigServiceTest {

    private MonitoringConfigService underTest;

    @Before
    public void setUp() {
        underTest = new MonitoringConfigService();
    }

    @Test
    public void testCreateMeteringConfigs() {
        // GIVEN
        MonitoringClusterType clusterType = MonitoringClusterType.CLOUDERA_MANAGER;
        MonitoringAuthConfig authConfig = new MonitoringAuthConfig("user", "pass".toCharArray());
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withCrn("myCrn").build();
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(clusterType, authConfig, clusterDetails);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myCrn", result.getClusterDetails().getCrn());
    }

    @Test
    public void testCreateMeteringConfigsWithNulls() {
        // GIVEN
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(null, null, null);
        // THEN
        assertFalse(result.isEnabled());
    }
}
