package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.telemetry.EnvironmentTelemetryDetails;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;

@ExtendWith(MockitoExtension.class)
class EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverterTest {

    private EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @Mock
    private EnvironmentFeatures environmentFeatures;

    @Mock
    private EnvironmentTelemetryDetails environmentTelemetryDetails;

    @BeforeEach()
    void setUp() {
        underTest = new EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter();
    }

    @Test
    void testNull() {
        UsageProto.CDPEnvironmentTelemetryFeatureDetails telemetryFeatureDetails = underTest.convert(null);

        assertEquals("", telemetryFeatureDetails.getClusterLogsCollection());
        assertEquals("", telemetryFeatureDetails.getWorkloadAnalytics());
        assertEquals("", telemetryFeatureDetails.getStorageLocationBase());
        assertEquals("", telemetryFeatureDetails.getBackupStorageLocationBase());
    }

    @Test
    void testConvertingEmptyEnvironmentDetails() {
        UsageProto.CDPEnvironmentTelemetryFeatureDetails telemetryFeatureDetails = underTest.convert(environmentDetails);

        assertEquals("", telemetryFeatureDetails.getClusterLogsCollection());
        assertEquals("", telemetryFeatureDetails.getWorkloadAnalytics());
        assertEquals("", telemetryFeatureDetails.getStorageLocationBase());
        assertEquals("", telemetryFeatureDetails.getBackupStorageLocationBase());
    }

    @Test
    void testConversionTelemetry() {
        FeatureSetting workloadAnalytics = new FeatureSetting();
        workloadAnalytics.setEnabled(Boolean.FALSE);
        EnvironmentTelemetryDetails telemetryDetails = new EnvironmentTelemetryDetails("storageLocationBase", "backupStorageLocationBase");

        when(environmentDetails.getEnvironmentTelemetryFeatures()).thenReturn(environmentFeatures);
        when(environmentFeatures.getWorkloadAnalytics()).thenReturn(workloadAnalytics);
        when(environmentDetails.getTelemetryDetails()).thenReturn(telemetryDetails);

        UsageProto.CDPEnvironmentTelemetryFeatureDetails telemetryFeatureDetails = underTest.convert(environmentDetails);

        assertEquals("", telemetryFeatureDetails.getClusterLogsCollection());
        assertEquals("false", telemetryFeatureDetails.getWorkloadAnalytics());
        assertEquals("storageLocationBase", telemetryFeatureDetails.getStorageLocationBase());
        assertEquals("backupStorageLocationBase", telemetryFeatureDetails.getBackupStorageLocationBase());
    }
}
