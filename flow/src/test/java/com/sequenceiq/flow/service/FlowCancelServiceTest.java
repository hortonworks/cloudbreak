package com.sequenceiq.flow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class FlowCancelServiceTest {

    @Mock
    private EventBus reactor;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private EventParameterFactory eventParameterFactory;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private Flow2Handler flow2Handler;

    @InjectMocks
    private FlowCancelService underTest;

    @Test
    void testCancelRunningFlows() {
        Map<String, Object> parameters = Map.of();
        when(eventParameterFactory.createEventParameters(1L)).thenReturn(parameters);
        when(eventFactory.createEventWithErrHandler(eq(parameters), any(Payload.class)))
                .thenAnswer(invocation -> {
                    Payload payload = invocation.getArgument(1, Payload.class);
                    assertEquals(Long.valueOf(1L), payload.getResourceId());
                    return new Event<>(payload);
                });

        underTest.cancelRunningFlows(1L);

        verify(reactor, times(1)).notify(eq(FlowConstants.FLOW_CANCEL), any(Event.class));
    }

    @Test
    void testCancelFlowSilentlyIgnoresException() throws TransactionExecutionException {
        doThrow(new TransactionExecutionException("asdf", new RuntimeException())).when(flow2Handler).cancelFlow(anyLong(), anyString());
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("asdf");
        flowLog.setResourceId(1L);

        underTest.cancelFlowSilently(flowLog);
    }

    @Test
    void testCancelFlow() throws TransactionExecutionException {
        FlowLog flowLog = new FlowLog();
        flowLog.setResourceId(1L);
        flowLog.setFlowId("flowid");

        underTest.cancelFlow(flowLog);

        verify(flow2Handler, times(1)).cancelFlow(flowLog.getResourceId(), flowLog.getFlowId());
    }

    @Test
    void testCancelTooOldTerminationFlowForResource() {
        underTest.cancelTooOldTerminationFlowForResource(1L, "asf");

        verify(flowLogService, times(1)).cancelTooOldTerminationFlowForResource(eq(1L), anyLong());
    }

    @Test
    void testCancelTooOldTerminationFlowIgnoresExceptions() {
        doThrow(new RuntimeException()).when(flowLogService).cancelTooOldTerminationFlowForResource(eq(1L), anyLong());

        underTest.cancelTooOldTerminationFlowForResource(1L, "asf");

        verify(flowLogService, times(1)).cancelTooOldTerminationFlowForResource(eq(1L), anyLong());
    }
}