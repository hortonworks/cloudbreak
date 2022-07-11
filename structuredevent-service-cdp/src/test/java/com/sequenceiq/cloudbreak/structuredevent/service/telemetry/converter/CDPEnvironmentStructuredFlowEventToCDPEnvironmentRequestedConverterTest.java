package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.CDPRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
class CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverterTest {

    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverter underTest;

    @BeforeEach()
    void setUp() {
        underTest = new CDPEnvironmentStructuredFlowEventToCDPEnvironmentRequestedConverter();
        CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        ReflectionTestUtils.setField(operationDetailsConverter, "appVersion", "version-1234");
        ReflectionTestUtils.setField(operationDetailsConverter, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
        ReflectionTestUtils.setField(underTest, "operationDetailsConverter", operationDetailsConverter);
        ReflectionTestUtils.setField(underTest, "environmentDetailsConverter", new EnvironmentDetailsToCDPEnvironmentDetailsConverter());
        ReflectionTestUtils.setField(underTest, "freeIPADetailsConverter", new EnvironmentDetailsToCDPFreeIPADetailsConverter());
        ReflectionTestUtils.setField(underTest, "telemetryFeatureDetailsConverter", new EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter());
    }

    @Test
    void testNullStructuredFlowEvent() {
        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(null);

        assertNotNull(environmentRequested.getOperationDetails());
        assertNotNull(environmentRequested.getEnvironmentDetails());
        assertNotNull(environmentRequested.getFreeIPA());
        assertNotNull(environmentRequested.getTelemetryFeatureDetails());
    }

    @Test
    void testConvertingEmptyStructuredFlowEvent() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        assertNotNull(environmentRequested.getOperationDetails());
        assertNotNull(environmentRequested.getEnvironmentDetails());
        assertNotNull(environmentRequested.getFreeIPA());
        assertNotNull(environmentRequested.getTelemetryFeatureDetails());
    }
}
