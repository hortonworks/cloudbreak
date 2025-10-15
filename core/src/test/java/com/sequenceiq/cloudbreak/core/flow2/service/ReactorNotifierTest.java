package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.config.EventBusStatisticReporter;
import com.sequenceiq.flow.service.FlowNameFormatService;

@ExtendWith(MockitoExtension.class)
public class ReactorNotifierTest {

    @Mock
    private EventBus reactor;

    @Mock
    private EventBusStatisticReporter reactorReporter;

    @Mock
    private StackService stackService;

    @Mock
    private EventParameterFactory eventParameterFactory;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private FlowNameFormatService flowNameFormatService;

    @Mock
    private NodeValidator nodeValidator;

    @InjectMocks
    private ReactorNotifier underTest;

    @BeforeEach
    public void init() {
        when(eventParameterFactory.createEventParameters(anyLong())).thenReturn(Map.of());
        lenient().doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testNonAllowedFlowInMaintenanceMode() {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.MAINTENANCE_MODE_ENABLED));
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        BaseFlowEvent baseFlowEvent = new BaseFlowEvent("dontcare", 1L, "crn");
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(new Event<Acceptable>(baseFlowEvent));

        assertThrows(CloudbreakServiceException.class, () -> underTest.notify(1L, "RANDOM", baseFlowEvent, stackService::getByIdWithTransaction));

        verify(reactor, never()).notify(anyString(), any(Event.class));
    }

    @Test
    public void testAccepted() throws InterruptedException {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        Acceptable data = mock(Acceptable.class);
        Promise<AcceptResult> accepted = (Promise<AcceptResult>) mock(Promise.class);
        when(data.accepted()).thenReturn(accepted);
        when(data.getResourceId()).thenReturn(1L);
        Event<Acceptable> event = new Event<>(data);
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(event);
        FlowAcceptResult acceptResult = FlowAcceptResult.runningInFlow("flowid");
        when(accepted.await(10L, TimeUnit.SECONDS)).thenReturn(acceptResult);

        underTest.notify(1L, "RANDOM", data, stackService::getByIdWithTransaction);

        verify(reactorReporter, times(1)).logInfoReport();
        verify(reactor, times(1)).notify(eq("RANDOM"), eq(event));
        verify(accepted, times(1)).await(10L, TimeUnit.SECONDS);
    }

    @Test
    public void testAcceptedReturnNull() throws InterruptedException {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        Acceptable data = mock(Acceptable.class);
        Promise<AcceptResult> accepted = (Promise<AcceptResult>) mock(Promise.class);
        when(data.accepted()).thenReturn(accepted);
        when(data.getResourceId()).thenReturn(1L);
        Event<Acceptable> event = new Event<>(data);
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(event);
        when(accepted.await(10L, TimeUnit.SECONDS)).thenReturn(null);

        assertThrows(FlowNotAcceptedException.class, () -> underTest.notify(1L, "RANDOM", data, stackService::getByIdWithTransaction));

        verify(reactorReporter, times(1)).logInfoReport();
        verify(reactorReporter, times(1)).logErrorReport();
        verify(reactor, times(1)).notify(eq("RANDOM"), eq(event));
        verify(accepted, times(1)).await(10L, TimeUnit.SECONDS);
    }

    @Test
    public void testAcceptedReturnFalse() throws InterruptedException {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        Acceptable data = mock(Acceptable.class);
        Promise<AcceptResult> accepted = (Promise<AcceptResult>) mock(Promise.class);
        when(data.accepted()).thenReturn(accepted);
        when(data.getResourceId()).thenReturn(1L);
        Event<Acceptable> event = new Event<>(data);
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(event);
        when(accepted.await(10L, TimeUnit.SECONDS)).thenReturn(FlowAcceptResult.alreadyExistingFlow(Collections.EMPTY_SET));

        assertThrows(FlowsAlreadyRunningException.class, () -> underTest.notify(1L, "RANDOM", data, stackService::getByIdWithTransaction));

        verify(reactorReporter, times(1)).logInfoReport();
        verify(reactorReporter, times(1)).logErrorReport();
        verify(reactor, times(1)).notify(eq("RANDOM"), eq(event));
        verify(accepted, times(1)).await(10L, TimeUnit.SECONDS);
    }
}