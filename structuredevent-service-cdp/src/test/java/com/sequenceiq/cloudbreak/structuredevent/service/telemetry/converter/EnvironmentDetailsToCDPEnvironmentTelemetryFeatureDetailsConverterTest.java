package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;

@ExtendWith(MockitoExtension.class)
public class EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverterTest {

    private EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @Mock
    private EnvironmentFeatures environmentFeatures;

    @BeforeEach()
    public void setUp() {
        underTest = new EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter();
    }

    @Test
    public void testNull() {
        UsageProto.CDPEnvironmentTelemetryFeatureDetails telemetryFeatureDetails = underTest.convert(null);

        Assertions.assertEquals("", telemetryFeatureDetails.getClusterLogsCollection());
        Assertions.assertEquals("", telemetryFeatureDetails.getWorkloadAnalytics());
    }

    @Test
    public void testConvertingEmptyEnvironmentDetails() {
        UsageProto.CDPEnvironmentTelemetryFeatureDetails telemetryFeatureDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals("", telemetryFeatureDetails.getClusterLogsCollection());
        Assertions.assertEquals("", telemetryFeatureDetails.getWorkloadAnalytics());
    }

    @Test
    public void testConversionTelemetry() {
        FeatureSetting clusterLogsCollection = new FeatureSetting();
        clusterLogsCollection.setEnabled(Boolean.TRUE);

        FeatureSetting workloadAnalytics = new FeatureSetting();
        workloadAnalytics.setEnabled(Boolean.FALSE);

        when(environmentDetails.getEnvironmentTelemetryFeatures()).thenReturn(environmentFeatures);
        when(environmentFeatures.getClusterLogsCollection()).thenReturn(clusterLogsCollection);
        when(environmentFeatures.getWorkloadAnalytics()).thenReturn(workloadAnalytics);

        UsageProto.CDPEnvironmentTelemetryFeatureDetails telemetryFeatureDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals("true", telemetryFeatureDetails.getClusterLogsCollection());
        Assertions.assertEquals("false", telemetryFeatureDetails.getWorkloadAnalytics());
    }
}
