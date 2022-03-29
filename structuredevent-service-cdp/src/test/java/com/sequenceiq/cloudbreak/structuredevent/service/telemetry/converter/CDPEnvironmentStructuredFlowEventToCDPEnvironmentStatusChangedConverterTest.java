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
public class CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverterTest {

    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter underTest;

    @BeforeEach()
    public void setUp() {
        underTest = new CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter();
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
        UsageProto.CDPEnvironmentStatusChanged environmentStatusChanged = underTest.convert(null,
                UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED);

        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED, environmentStatusChanged.getNewStatus());
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, environmentStatusChanged.getOldStatus());
        Assertions.assertNotNull(environmentStatusChanged.getOperationDetails());
        Assertions.assertNotNull(environmentStatusChanged.getEnvironmentDetails());
        Assertions.assertNotNull(environmentStatusChanged.getFreeIPA());
        Assertions.assertNotNull(environmentStatusChanged.getTelemetryFeatureDetails());
    }

    @Test
    public void testConvertingEmptyStructuredFlowEvent() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        UsageProto.CDPEnvironmentStatusChanged environmentStatusChanged = underTest.convert(cdpStructuredFlowEvent,
                UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED);

        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED, environmentStatusChanged.getNewStatus());
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, environmentStatusChanged.getOldStatus());
        Assertions.assertNotNull(environmentStatusChanged.getOperationDetails());
        Assertions.assertNotNull(environmentStatusChanged.getEnvironmentDetails());
        Assertions.assertNotNull(environmentStatusChanged.getFreeIPA());
        Assertions.assertNotNull(environmentStatusChanged.getTelemetryFeatureDetails());
    }
}
