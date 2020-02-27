package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.config.EventBusStatisticReporter;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;

@RunWith(MockitoJUnitRunner.class)
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

    @InjectMocks
    private ReactorNotifier underTest;

    @Before
    public void init() {
        when(eventParameterFactory.createEventParameters(anyLong())).thenReturn(Map.of());
    }

    @Test(expected = CloudbreakApiException.class)
    public void testNonAllowedFlowInMaintenanceMode() {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setStatus(Status.MAINTENANCE_MODE_ENABLED);
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        BaseFlowEvent baseFlowEvent = new BaseFlowEvent("dontcare", 1L, "crn");
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(new Event<Acceptable>(baseFlowEvent));

        underTest.notify(1L, "RANDOM", baseFlowEvent, stackService::getByIdWithTransaction);

        verify(reactor, never()).notify(anyString(), any(Event.class));
    }

    @Test
    public void testAccepted() throws InterruptedException {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setStatus(Status.AVAILABLE);
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

    @Test(expected = FlowNotAcceptedException.class)
    public void testAcceptedReturnNull() throws InterruptedException {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setStatus(Status.AVAILABLE);
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        Acceptable data = mock(Acceptable.class);
        Promise<AcceptResult> accepted = (Promise<AcceptResult>) mock(Promise.class);
        when(data.accepted()).thenReturn(accepted);
        when(data.getResourceId()).thenReturn(1L);
        Event<Acceptable> event = new Event<>(data);
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(event);
        when(accepted.await(10L, TimeUnit.SECONDS)).thenReturn(null);

        underTest.notify(1L, "RANDOM", data, stackService::getByIdWithTransaction);

        verify(reactorReporter, times(1)).logInfoReport();
        verify(reactorReporter, times(1)).logErrorReport();
        verify(reactor, times(1)).notify(eq("RANDOM"), eq(event));
        verify(accepted, times(1)).await(10L, TimeUnit.SECONDS);
    }

    @Test(expected = FlowsAlreadyRunningException.class)
    public void testAcceptedReturnFalse() throws InterruptedException {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.getCluster().setStatus(Status.AVAILABLE);
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        Acceptable data = mock(Acceptable.class);
        Promise<AcceptResult> accepted = (Promise<AcceptResult>) mock(Promise.class);
        when(data.accepted()).thenReturn(accepted);
        when(data.getResourceId()).thenReturn(1L);
        Event<Acceptable> event = new Event<>(data);
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(event);
        when(accepted.await(10L, TimeUnit.SECONDS)).thenReturn(FlowAcceptResult.alreadyExistingFlow());

        underTest.notify(1L, "RANDOM", data, stackService::getByIdWithTransaction);

        verify(reactorReporter, times(1)).logInfoReport();
        verify(reactorReporter, times(1)).logErrorReport();
        verify(reactor, times(1)).notify(eq("RANDOM"), eq(event));
        verify(accepted, times(1)).await(10L, TimeUnit.SECONDS);
    }
}