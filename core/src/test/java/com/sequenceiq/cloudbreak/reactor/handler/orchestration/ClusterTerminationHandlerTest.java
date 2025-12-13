package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class ClusterTerminationHandlerTest {

    public static final long STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private MeteringService meteringService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private ClusterTerminationHandler underTest;

    private Event<ClusterTerminationRequest> event = new Event<>(new ClusterTerminationRequest(STACK_ID, STACK_ID, false));

    private Event<ClusterTerminationRequest> eventForced = new Event<>(new ClusterTerminationRequest(STACK_ID, STACK_ID, true));

    @Test
    void testAcceptEventThrowsException() {
        when(stackDtoService.getByIdWithoutResources(anyLong())).thenThrow(new RuntimeException("the message"));

        underTest.accept(event);

        ArgumentCaptor<Event<ClusterTerminationResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        ClusterTerminationResult result = (ClusterTerminationResult) resultEvent.getData();
        assertNotNull(result.getErrorDetails());
        assertNotNull(result.getStatusReason());
        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    @Test
    void testResultWhenExceptionThrownAndForcedFlag() {
        when(stackDtoService.getByIdWithoutResources(anyLong())).thenThrow(new RuntimeException("the message"));

        underTest.accept(eventForced);

        ArgumentCaptor<Event<ClusterTerminationResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        ClusterTerminationResult result = (ClusterTerminationResult) resultEvent.getData();
        assertNull(result.getException());
        assertNull(result.getErrorDetails());
        assertEquals(EventStatus.OK, result.getStatus());
    }

}