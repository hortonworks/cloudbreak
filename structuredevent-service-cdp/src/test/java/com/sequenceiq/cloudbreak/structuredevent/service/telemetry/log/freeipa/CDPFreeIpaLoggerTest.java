package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeIpaStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class CDPFreeIpaLoggerTest {

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private FreeIpaUseCaseMapper freeIpaUseCaseMapper;

    @Mock
    private CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter statusChangedConverter;

    @InjectMocks
    private CDPFreeIpaLogger underTest;

    @Test
    void testLog() {
        CDPFreeIpaStructuredFlowEvent cdpFreeIpaStructuredFlowEvent = mock(CDPFreeIpaStructuredFlowEvent.class);
        FlowDetails flowDetails = mock(FlowDetails.class);
        CDPOperationDetails operationDetails = mock(CDPOperationDetails.class);
        UsageProto.CDPFreeIPAStatusChanged cdpFreeIPAStatusChanged = mock(UsageProto.CDPFreeIPAStatusChanged.class);
        when(cdpFreeIpaStructuredFlowEvent.getFlow()).thenReturn(flowDetails);
        when(cdpFreeIpaStructuredFlowEvent.getOperation()).thenReturn(operationDetails);
        when(operationDetails.getResourceType()).thenReturn(CloudbreakEventService.FREEIPA_RESOURCE_TYPE);
        when(freeIpaUseCaseMapper.useCase(flowDetails)).thenReturn(UsageProto.CDPFreeIPAStatus.Value.CREATE_FINISHED);
        when(statusChangedConverter.convert(cdpFreeIpaStructuredFlowEvent, UsageProto.CDPFreeIPAStatus.Value.CREATE_FINISHED))
                .thenReturn(cdpFreeIPAStatusChanged);

        underTest.log(cdpFreeIpaStructuredFlowEvent);

        verify(usageReporter).cdpFreeIpaStatusChanged(cdpFreeIPAStatusChanged);
    }

    @Test
    void testLogWhenUseCaseIsUnset() {
        CDPFreeIpaStructuredFlowEvent cdpFreeIpaStructuredFlowEvent = mock(CDPFreeIpaStructuredFlowEvent.class);
        FlowDetails flowDetails = mock(FlowDetails.class);
        when(cdpFreeIpaStructuredFlowEvent.getFlow()).thenReturn(flowDetails);
        when(freeIpaUseCaseMapper.useCase(flowDetails)).thenReturn(UsageProto.CDPFreeIPAStatus.Value.UNSET);

        underTest.log(cdpFreeIpaStructuredFlowEvent);

        verifyNoInteractions(statusChangedConverter, usageReporter);
    }

    @Test
    void testAcceptableEventClass() {
        assertEquals(CDPFreeIpaStructuredFlowEvent.class, underTest.acceptableEventClass());
    }

}
