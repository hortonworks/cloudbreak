package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

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
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(clusterType, authConfig);
        // THEN
        assertTrue(result.isEnabled());
    }

    @Test
    public void testCreateMeteringConfigsWithNulls() {
        // GIVEN
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(null, null);
        // THEN
        assertFalse(result.isEnabled());
    }
}
