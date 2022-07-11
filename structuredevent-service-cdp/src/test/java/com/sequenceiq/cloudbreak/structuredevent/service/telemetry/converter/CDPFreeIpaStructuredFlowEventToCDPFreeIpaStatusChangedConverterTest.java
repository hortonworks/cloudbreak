package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeIpaStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.CDPRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
class CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverterTest {

    private CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter underTest;

    @BeforeEach()
    void setUp() {
        underTest = new CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter();
        CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        ReflectionTestUtils.setField(operationDetailsConverter, "appVersion", "version-1234");
        ReflectionTestUtils.setField(operationDetailsConverter, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
        ReflectionTestUtils.setField(underTest, "operationDetailsConverter", operationDetailsConverter);
        StackDetailsToCDPFreeIPAExtendedDetailsConverter freeIPAExtendedDetailsConverter = new StackDetailsToCDPFreeIPAExtendedDetailsConverter();
        ReflectionTestUtils.setField(freeIPAExtendedDetailsConverter, "freeIPAShapeConverter", new StackDetailsToCDPFreeIPAShapeConverter());
        ReflectionTestUtils.setField(freeIPAExtendedDetailsConverter, "imageDetailsConverter", new StackDetailsToCDPImageDetailsConverter());
        ReflectionTestUtils.setField(underTest, "freeIPAExtendedDetailsConverter", freeIPAExtendedDetailsConverter);
        ReflectionTestUtils.setField(underTest, "freeIPAStatusDetailsConverter", new StackDetailsToCDPFreeIPAStatusDetailsConverter());
    }

    @Test
    void testNullStructuredFlowEvent() {
        UsageProto.CDPFreeIPAStatusChanged cdpFreeIPAStatusChanged = underTest.convert(null,
                UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED);

        assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED, cdpFreeIPAStatusChanged.getNewStatus());
        assertNotNull(cdpFreeIPAStatusChanged.getOperationDetails());
        assertNotNull(cdpFreeIPAStatusChanged.getFreeIPADetails());
        assertNotNull(cdpFreeIPAStatusChanged.getStatusDetails());
        assertEquals("", cdpFreeIPAStatusChanged.getEnvironmentCrn());
    }

    @Test
    void testConvertingEmptyStructuredFlowEvent() {
        CDPFreeIpaStructuredFlowEvent cdpStructuredFlowEvent = new CDPFreeIpaStructuredFlowEvent();
        UsageProto.CDPFreeIPAStatusChanged cdpFreeIPAStatusChanged = underTest.convert(cdpStructuredFlowEvent,
                UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED);

        assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED, cdpFreeIPAStatusChanged.getNewStatus());
        assertNotNull(cdpFreeIPAStatusChanged.getOperationDetails());
        assertNotNull(cdpFreeIPAStatusChanged.getFreeIPADetails());
        assertNotNull(cdpFreeIPAStatusChanged.getStatusDetails());
        assertEquals("", cdpFreeIPAStatusChanged.getEnvironmentCrn());
    }

    @Test
    void testConvertingNotEmptyStructuredFlowEvent() {
        CDPFreeIpaStructuredFlowEvent cdpStructuredFlowEvent = new CDPFreeIpaStructuredFlowEvent();
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setEnvironmentCrn("testcrn");
        cdpStructuredFlowEvent.setOperation(operationDetails);
        UsageProto.CDPFreeIPAStatusChanged cdpFreeIPAStatusChanged = underTest.convert(cdpStructuredFlowEvent,
                UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED);

        assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED, cdpFreeIPAStatusChanged.getNewStatus());
        assertNotNull(cdpFreeIPAStatusChanged.getOperationDetails());
        assertNotNull(cdpFreeIPAStatusChanged.getFreeIPADetails());
        assertNotNull(cdpFreeIPAStatusChanged.getStatusDetails());
        assertEquals("testcrn", cdpFreeIPAStatusChanged.getEnvironmentCrn());
    }
}
