package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverterTest {

    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter underTest;

    @BeforeEach()
    void setUp() {
        underTest = new CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter();
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
        UsageProto.CDPEnvironmentStatusChanged environmentStatusChanged = underTest.convert(null,
                UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED);

        assertEquals(UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED, environmentStatusChanged.getNewStatus());
        assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, environmentStatusChanged.getOldStatus());
        assertNotNull(environmentStatusChanged.getOperationDetails());
        assertNotNull(environmentStatusChanged.getEnvironmentDetails());
        assertNotNull(environmentStatusChanged.getFreeIPA());
        assertNotNull(environmentStatusChanged.getTelemetryFeatureDetails());
    }

    @Test
    void testConvertingEmptyStructuredFlowEvent() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        UsageProto.CDPEnvironmentStatusChanged environmentStatusChanged = underTest.convert(cdpStructuredFlowEvent,
                UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED);

        assertEquals(UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED, environmentStatusChanged.getNewStatus());
        assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, environmentStatusChanged.getOldStatus());
        assertNotNull(environmentStatusChanged.getOperationDetails());
        assertNotNull(environmentStatusChanged.getEnvironmentDetails());
        assertNotNull(environmentStatusChanged.getFreeIPA());
        assertNotNull(environmentStatusChanged.getTelemetryFeatureDetails());
    }
}
