package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.RequestProcessingStepMapper;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;

@ExtendWith(MockitoExtension.class)
class CDPStructuredFlowEventToCDPEnvironmentRequestedConverterTest {

    private CDPStructuredFlowEventToCDPEnvironmentRequestedConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @Mock
    private EnvironmentFeatures environmentFeatures;

    @BeforeEach()
    public void setUp() {
        underTest = new CDPStructuredFlowEventToCDPEnvironmentRequestedConverter();
        CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(operationDetailsConverter, "appVersion", "version-1234");
        Whitebox.setInternalState(operationDetailsConverter, "requestProcessingStepMapper", new RequestProcessingStepMapper());
        Whitebox.setInternalState(underTest, "operationDetailsConverter", operationDetailsConverter);
    }

    @Test
    public void testConversionWithNull() {
        Assert.assertNull("We should return with null if the input is null", underTest.convert(null));
    }

    @Test
    public void testConversionWithoutTelemetry() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("", environmentRequested.getTelemetryFeatureDetails().getClusterLogsCollection());
        Assert.assertEquals("", environmentRequested.getTelemetryFeatureDetails().getWorkloadAnalytics());
    }

    @Test
    public void testConversionTelemetry() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        FeatureSetting clusterLogsCollection = new FeatureSetting();
        clusterLogsCollection.setEnabled(Boolean.TRUE);

        FeatureSetting workloadAnalytics = new FeatureSetting();
        workloadAnalytics.setEnabled(Boolean.FALSE);


        when(environmentDetails.getEnvironmentTelemetryFeatures()).thenReturn(environmentFeatures);
        when(environmentFeatures.getClusterLogsCollection()).thenReturn(clusterLogsCollection);
        when(environmentFeatures.getWorkloadAnalytics()).thenReturn(workloadAnalytics);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("true", environmentRequested.getTelemetryFeatureDetails().getClusterLogsCollection());
        Assert.assertEquals("false", environmentRequested.getTelemetryFeatureDetails().getWorkloadAnalytics());
    }
}