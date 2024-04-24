package com.sequenceiq.cloudbreak.telemetry.metering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.context.MeteringContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class MeteringConfigServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private MeteringConfigService underTest;

    @BeforeEach
    public void setUp() {
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "app", "stream", false);
        TelemetryComponentUpgradeConfiguration meteringAgentConfig = new TelemetryComponentUpgradeConfiguration();
        meteringAgentConfig.setDesiredDate("2021-01-01");
        TelemetryUpgradeConfiguration upgradeConfigs = new TelemetryUpgradeConfiguration();
        upgradeConfigs.setEnabled(true);
        upgradeConfigs.setMeteringAgent(meteringAgentConfig);
        underTest = new MeteringConfigService(meteringConfiguration, upgradeConfigs);
    }

    @Test
    public void testIsEnabled() {
        // GIVEN
        // WHEN
        boolean result = underTest.isEnabled(telemetryContext());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsEnabledWithoutTelemetry() {
        // GIVEN
        TelemetryContext context = telemetryContext();
        context.setTelemetry(null);
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWithoutContext() {
        // GIVEN
        // WHEN
        boolean result = underTest.isEnabled(null);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCreateMeteringConfigs() {
        // GIVEN
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext()).toMap();
        // THEN
        assertEquals(true, result.get("enabled"));
        assertEquals("DATAHUB", result.get("serviceType"));
        assertEquals("stream", result.get("streamName"));
    }

    private TelemetryContext telemetryContext() {
        TelemetryContext context = new TelemetryContext();
        context.setClusterType(FluentClusterType.DATAHUB);
        MeteringContext meteringContext = MeteringContext.builder()
                .enabled()
                .withVersion("7.2.16")
                .withServiceType(FluentClusterType.DATAHUB.value())
                .build();
        context.setMeteringContext(meteringContext);
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withPlatform("AWS")
                .withCrn("crn")
                .withEnvironmentCrn(ENVIRONMENT_CRN)
                .build();
        context.setClusterDetails(clusterDetails);
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        features.addMetering(true);
        telemetry.setFeatures(features);
        context.setTelemetry(telemetry);
        return context;
    }
}
