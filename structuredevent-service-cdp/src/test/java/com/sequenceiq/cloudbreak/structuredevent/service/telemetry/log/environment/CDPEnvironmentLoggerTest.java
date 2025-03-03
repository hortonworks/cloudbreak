package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

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
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class CDPEnvironmentLoggerTest {

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private EnvironmentUseCaseMapper environmentUseCaseMapper;

    @Mock
    private CDPEnvironmentStructuredFlowEventToCDPEnvironmentStatusChangedConverter statusChangedConverter;

    @InjectMocks
    private CDPEnvironmentLogger underTest;

    @Test
    void testLog() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = mock(CDPEnvironmentStructuredFlowEvent.class);
        FlowDetails flowDetails = mock(FlowDetails.class);
        CDPOperationDetails operationDetails = mock(CDPOperationDetails.class);
        UsageProto.CDPEnvironmentStatusChanged cdpEnvironmentStatusChanged = mock(UsageProto.CDPEnvironmentStatusChanged.class);
        when(cdpStructuredFlowEvent.getFlow()).thenReturn(flowDetails);
        when(cdpStructuredFlowEvent.getOperation()).thenReturn(operationDetails);
        when(operationDetails.getResourceType()).thenReturn(CloudbreakEventService.ENVIRONMENT_RESOURCE_TYPE);
        when(environmentUseCaseMapper.useCase(flowDetails)).thenReturn(UsageProto.CDPEnvironmentStatus.Value.CREATE_FINISHED);
        when(statusChangedConverter.convert(cdpStructuredFlowEvent, UsageProto.CDPEnvironmentStatus.Value.CREATE_FINISHED))
                .thenReturn(cdpEnvironmentStatusChanged);

        underTest.log(cdpStructuredFlowEvent);

        verify(usageReporter).cdpEnvironmentStatusChanged(cdpEnvironmentStatusChanged);
    }

    @Test
    void testLogWhenUseCaseIsUnset() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = mock(CDPEnvironmentStructuredFlowEvent.class);
        FlowDetails flowDetails = mock(FlowDetails.class);
        when(cdpStructuredFlowEvent.getFlow()).thenReturn(flowDetails);
        when(environmentUseCaseMapper.useCase(flowDetails)).thenReturn(UsageProto.CDPEnvironmentStatus.Value.UNSET);

        underTest.log(cdpStructuredFlowEvent);

        verifyNoInteractions(statusChangedConverter, usageReporter);
    }

    @Test
    void testAcceptableEventClass() {
        assertEquals(CDPEnvironmentStructuredFlowEvent.class, underTest.acceptableEventClass());
    }
}
