package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stopreplication.StopFreeIpaReplicationRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaServicesStopService;

@ExtendWith(MockitoExtension.class)
class StopFreeIpaReplicationHandlerTest {

    @Mock
    private FreeIpaServicesStopService freeIpaServicesStopService;

    @InjectMocks
    private StopFreeIpaReplicationHandler underTest;

    @Test
    void testDoAcceptStopsServicesAndReturnsFinished() throws Exception {
        Long resourceId = 1L;
        List<String> instanceIds = List.of("i-001", "i-002");
        StopFreeIpaReplicationRequest request = new StopFreeIpaReplicationRequest(resourceId, instanceIds);
        HandlerEvent<StopFreeIpaReplicationRequest> event = mock(HandlerEvent.class);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);

        verify(freeIpaServicesStopService).stopServicesOnInstances(eq(resourceId), eq(instanceIds));
        assertInstanceOf(StackEvent.class, result);
        assertEquals(DownscaleFlowEvent.STOP_FREEIPA_REPLICATION_FINISHED.event(), result.getSelector());
        assertEquals(resourceId, ((StackEvent) result).getResourceId());
    }

    @Test
    void testDoAcceptSwallowsServiceExceptionAndReturnsFinished() throws Exception {
        Long resourceId = 1L;
        List<String> instanceIds = List.of("i-001");
        StopFreeIpaReplicationRequest request = new StopFreeIpaReplicationRequest(resourceId, instanceIds);
        HandlerEvent<StopFreeIpaReplicationRequest> event = mock(HandlerEvent.class);
        when(event.getData()).thenReturn(request);
        doThrow(new RuntimeException("boom")).when(freeIpaServicesStopService)
                .stopServicesOnInstances(eq(resourceId), eq(instanceIds));

        Selectable result = underTest.doAccept(event);

        assertEquals(DownscaleFlowEvent.STOP_FREEIPA_REPLICATION_FINISHED.event(), result.getSelector());
        assertEquals(resourceId, ((StackEvent) result).getResourceId());
    }

    @Test
    void testDefaultFailureEvent() {
        Long resourceId = 1L;
        Exception exception = new Exception("Failure");
        Event<StopFreeIpaReplicationRequest> event = mock(Event.class);

        Selectable result = underTest.defaultFailureEvent(resourceId, exception, event);

        assertInstanceOf(DownscaleFailureEvent.class, result);
        DownscaleFailureEvent failureEvent = (DownscaleFailureEvent) result;
        assertEquals(resourceId, failureEvent.getResourceId());
        assertEquals(exception, failureEvent.getException());
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(StopFreeIpaReplicationRequest.class), underTest.selector());
    }
}
