package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;

@ExtendWith(MockitoExtension.class)
class CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverterTest {

    @Mock
    private StackDetailsToCDPFreeIPAExtendedDetailsConverter freeIPAExtendedDetailsConverter;

    @Mock
    private StackDetailsToCDPFreeIPAStatusDetailsConverter freeIPAStatusDetailsConverter;

    @InjectMocks
    private CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverter underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "appVersion", "version-1234");
    }

    @Test
    void testConvert() {
        ReflectionTestUtils.setField(underTest, "appVersion", "version-1234");
        CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent = new CDPFreeipaStructuredSyncEvent();
        CDPOperationDetails cdpOperationDetails = new CDPOperationDetails();
        cdpOperationDetails.setAccountId("accountId");
        cdpOperationDetails.setResourceCrn("resourceCrn");
        cdpOperationDetails.setResourceName("resourceName");
        cdpOperationDetails.setUserCrn("userCrn");
        cdpOperationDetails.setUuid("uuid");
        cdpOperationDetails.setEnvironmentCrn("envCrn");
        cdpFreeipaStructuredSyncEvent.setOperation(cdpOperationDetails);
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCloudPlatform("AWS");
        cdpFreeipaStructuredSyncEvent.setStackDetails(stackDetails);
        UsageProto.CDPFreeIPAExtendedDetails cdpFreeIPAExtendedDetails = mock(UsageProto.CDPFreeIPAExtendedDetails.class);
        UsageProto.CDPFreeIPAStatusDetails cdpFreeIPAStatusDetails = mock(UsageProto.CDPFreeIPAStatusDetails.class);
        when(freeIPAExtendedDetailsConverter.convert(stackDetails)).thenReturn(cdpFreeIPAExtendedDetails);
        when(freeIPAStatusDetailsConverter.convert(stackDetails)).thenReturn(cdpFreeIPAStatusDetails);

        UsageProto.CDPFreeIPASync result = underTest.convert(cdpFreeipaStructuredSyncEvent);

        assertEquals("accountId", result.getOperationDetails().getAccountId());
        assertEquals("resourceCrn", result.getOperationDetails().getResourceCrn());
        assertEquals("resourceName", result.getOperationDetails().getResourceName());
        assertEquals("userCrn", result.getOperationDetails().getInitiatorCrn());
        assertEquals("uuid", result.getOperationDetails().getCorrelationId());
        assertEquals("version-1234", result.getOperationDetails().getApplicationVersion());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, result.getOperationDetails().getEnvironmentType());
        assertEquals("envCrn", result.getEnvironmentCrn());
        assertEquals(cdpFreeIPAExtendedDetails, result.getFreeIPADetails());
        assertEquals(cdpFreeIPAStatusDetails, result.getStatusDetails());
    }

}
