package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeIpaStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.CDPRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
public class CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverterTest {

    private CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter underTest;

    @BeforeEach()
    public void setUp() {
        underTest = new CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter();
        CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(operationDetailsConverter, "appVersion", "version-1234");
        Whitebox.setInternalState(operationDetailsConverter, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
        Whitebox.setInternalState(underTest, "operationDetailsConverter", operationDetailsConverter);
        StackDetailsToCDPFreeIPAExtendedDetailsConverter freeIPAExtendedDetailsConverter = new StackDetailsToCDPFreeIPAExtendedDetailsConverter();
        Whitebox.setInternalState(freeIPAExtendedDetailsConverter, "freeIPAShapeConverter", new StackDetailsToCDPFreeIPAShapeConverter());
        Whitebox.setInternalState(freeIPAExtendedDetailsConverter, "imageDetailsConverter", new StackDetailsToCDPImageDetailsConverter());
        Whitebox.setInternalState(underTest, "freeIPAExtendedDetailsConverter", freeIPAExtendedDetailsConverter);
        Whitebox.setInternalState(underTest, "freeIPAStatusDetailsConverter", new StackDetailsToCDPFreeIPAStatusDetailsConverter());
    }

    @Test
    public void testNullStructuredFlowEvent() {
        UsageProto.CDPFreeIPAStatusChanged cdpFreeIPAStatusChanged = underTest.convert(null,
                UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED);

        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED, cdpFreeIPAStatusChanged.getNewStatus());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getOperationDetails());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getFreeIPADetails());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getStatusDetails());
        Assertions.assertEquals("", cdpFreeIPAStatusChanged.getEnvironmentCrn());
    }

    @Test
    public void testConvertingEmptyStructuredFlowEvent() {
        CDPFreeIpaStructuredFlowEvent cdpStructuredFlowEvent = new CDPFreeIpaStructuredFlowEvent();
        UsageProto.CDPFreeIPAStatusChanged cdpFreeIPAStatusChanged = underTest.convert(cdpStructuredFlowEvent,
                UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED);

        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED, cdpFreeIPAStatusChanged.getNewStatus());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getOperationDetails());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getFreeIPADetails());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getStatusDetails());
        Assertions.assertEquals("", cdpFreeIPAStatusChanged.getEnvironmentCrn());
    }

    @Test
    public void testConvertingNotEmptyStructuredFlowEvent() {
        CDPFreeIpaStructuredFlowEvent cdpStructuredFlowEvent = new CDPFreeIpaStructuredFlowEvent();
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setEnvironmentCrn("testcrn");
        cdpStructuredFlowEvent.setOperation(operationDetails);
        UsageProto.CDPFreeIPAStatusChanged cdpFreeIPAStatusChanged = underTest.convert(cdpStructuredFlowEvent,
                UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED);

        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED, cdpFreeIPAStatusChanged.getNewStatus());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getOperationDetails());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getFreeIPADetails());
        Assertions.assertNotNull(cdpFreeIPAStatusChanged.getStatusDetails());
        Assertions.assertEquals("testcrn", cdpFreeIPAStatusChanged.getEnvironmentCrn());
    }
}
