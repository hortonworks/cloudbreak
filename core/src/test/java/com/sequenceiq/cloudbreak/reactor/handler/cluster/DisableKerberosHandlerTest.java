package com.sequenceiq.cloudbreak.reactor.handler.cluster;

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
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class DisableKerberosHandlerTest {

    public static final long STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private DisableKerberosHandler underTest;

    private Event<DisableKerberosRequest> event = new Event<>(new DisableKerberosRequest(STACK_ID, false));

    private Event<DisableKerberosRequest> eventForced = new Event<>(new DisableKerberosRequest(STACK_ID, true));

    @Test
    void testAcceptEventThrowsException() {
        when(stackDtoService.getById(anyLong())).thenReturn(new StackDto());
        when(clusterApiConnectors.getConnector((StackDtoDelegate) any())).thenThrow(new RuntimeException("the message"));

        underTest.accept(event);

        ArgumentCaptor<Event<DisableKerberosResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        DisableKerberosResult result = (DisableKerberosResult) resultEvent.getData();
        assertNotNull(result.getErrorDetails());
        assertNotNull(result.getStatusReason());
        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    @Test
    void testResultWhenExceptionThrownAndForcedFlag() {
        when(stackDtoService.getById(anyLong())).thenReturn(new StackDto());
        when(clusterApiConnectors.getConnector((StackDtoDelegate) any())).thenThrow(new RuntimeException("the message"));

        underTest.accept(eventForced);

        ArgumentCaptor<Event<DisableKerberosResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        DisableKerberosResult result = (DisableKerberosResult) resultEvent.getData();
        assertNull(result.getException());
        assertNull(result.getErrorDetails());
        assertEquals(EventStatus.OK, result.getStatus());
    }

}