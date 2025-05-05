package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stophealthagent.StopHealthAgentRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.healthagent.HealthAgentService;

@ExtendWith(MockitoExtension.class)
class StopHealthAgentHandlerTest {

    @Mock
    private HealthAgentService healthAgentService;

    @Mock
    private DelayedExecutorService delayedExecutorService;

    @InjectMocks
    private StopHealthAgentHandler underTest;

    @Test
    void testDoAccept() throws ExecutionException, InterruptedException {
        Long resourceId = 1L;
        List<String> fqdns = List.of("host1.example.com", "host2.example.com");
        StopHealthAgentRequest request = new StopHealthAgentRequest(resourceId, fqdns);
        HandlerEvent<StopHealthAgentRequest> event = mock(HandlerEvent.class);

        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);

        verify(healthAgentService).stopHealthAgentOnHosts(eq(resourceId), eq(Set.copyOf(fqdns)));
        verify(delayedExecutorService).runWithDelay(any(Runnable.class), eq(60L), eq(TimeUnit.SECONDS));
        assertInstanceOf(StackEvent.class, result);
        assertEquals(DownscaleFlowEvent.STOP_HEALTH_AGENT_FINISHED.event(), result.getSelector());
        assertEquals(resourceId, ((StackEvent) result).getResourceId());
    }

    @Test
    void testDoAcceptWithException() throws ExecutionException, InterruptedException {
        Long resourceId = 1L;
        List<String> fqdns = List.of("host1.example.com");
        StopHealthAgentRequest request = new StopHealthAgentRequest(resourceId, fqdns);
        HandlerEvent<StopHealthAgentRequest> event = mock(HandlerEvent.class);

        when(event.getData()).thenReturn(request);
        doThrow(new ExecutionException("Error", new Exception())).when(delayedExecutorService)
                .runWithDelay(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));

        assertThrows(RuntimeException.class, () -> underTest.doAccept(event));
    }

    @Test
    void testDefaultFailureEvent() {
        Long resourceId = 1L;
        Exception exception = new Exception("Failure");
        Event<StopHealthAgentRequest> event = mock(Event.class);

        Selectable result = underTest.defaultFailureEvent(resourceId, exception, event);

        assertInstanceOf(DownscaleFailureEvent.class, result);
        DownscaleFailureEvent failureEvent = (DownscaleFailureEvent) result;
        assertEquals(resourceId, failureEvent.getResourceId());
        assertEquals(exception.getMessage(), failureEvent.getException().getMessage());
        assertEquals(exception, failureEvent.getException());
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(StopHealthAgentRequest.class), underTest.selector());
    }
}