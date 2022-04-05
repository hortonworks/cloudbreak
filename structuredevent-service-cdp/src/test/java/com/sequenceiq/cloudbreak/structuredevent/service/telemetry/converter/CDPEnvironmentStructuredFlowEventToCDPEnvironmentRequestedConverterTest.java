package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.CDPRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
class CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverterTest {

    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverter underTest;

    @BeforeEach()
    public void setUp() {
        underTest = new CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverter();
        CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(operationDetailsConverter, "appVersion", "version-1234");
        Whitebox.setInternalState(operationDetailsConverter, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
        Whitebox.setInternalState(underTest, "operationDetailsConverter", operationDetailsConverter);
        Whitebox.setInternalState(underTest, "environmentDetailsConverter", new EnvironmentDetailsToCDPEnvironmentDetailsConverter());
        Whitebox.setInternalState(underTest, "freeIPADetailsConverter", new EnvironmentDetailsToCDPFreeIPADetailsConverter());
        Whitebox.setInternalState(underTest, "telemetryFeatureDetailsConverter", new EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter());
    }

    @Test
    public void testNullStructuredFlowEvent() {
        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(null);

        Assertions.assertNotNull(environmentRequested.getOperationDetails());
        Assertions.assertNotNull(environmentRequested.getEnvironmentDetails());
        Assertions.assertNotNull(environmentRequested.getFreeIPA());
        Assertions.assertNotNull(environmentRequested.getTelemetryFeatureDetails());
    }

    @Test
    public void testConvertingEmptyStructuredFlowEvent() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assertions.assertNotNull(environmentRequested.getOperationDetails());
        Assertions.assertNotNull(environmentRequested.getEnvironmentDetails());
        Assertions.assertNotNull(environmentRequested.getFreeIPA());
        Assertions.assertNotNull(environmentRequested.getTelemetryFeatureDetails());
    }
}