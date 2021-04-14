package com.sequenceiq.freeipa.flow.stack;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.start.FreeIpaServiceStartService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.StackStatusCheckerJob;

@ExtendWith(MockitoExtension.class)
class HealthCheckHandlerTest {

    @Mock
    private FreeIpaServiceStartService freeIpaServiceStartService;

    @Mock
    private StackService stackService;

    @Mock
    private StackStatusCheckerJob stackStatusCheckerJob;

    @InjectMocks
    private HealthCheckHandler underTest;

    @Test
    void testHealthCheckSuccess() {
        Stack stack = mock(Stack.class);
        HandlerEvent<HealthCheckRequest> event = mock(HandlerEvent.class);
        HealthCheckRequest request = new HealthCheckRequest(1L, true);

        when(event.getData()).thenReturn(request);
        when(stackService.getByIdWithListsInTransaction(any())).thenReturn(stack);

        assertTrue(underTest.doAccept(event) instanceof HealthCheckSuccess);

        verify(freeIpaServiceStartService).pollFreeIpaHealth(eq(stack));
        verify(stackStatusCheckerJob).syncAStack(eq(stack), eq(true));
    }

    @Test
    void testHealthCheckWithoutWaitingSuccess() {
        Stack stack = mock(Stack.class);
        HandlerEvent<HealthCheckRequest> event = mock(HandlerEvent.class);
        HealthCheckRequest request = new HealthCheckRequest(1L, false);

        when(event.getData()).thenReturn(request);
        when(stackService.getByIdWithListsInTransaction(any())).thenReturn(stack);

        assertTrue(underTest.doAccept(event) instanceof HealthCheckSuccess);

        verify(freeIpaServiceStartService, never()).pollFreeIpaHealth(eq(stack));
        verify(stackStatusCheckerJob).syncAStack(eq(stack), eq(true));
    }

    @Test
    void testHealthCheckFailure() {
        HandlerEvent<HealthCheckRequest> event = mock(HandlerEvent.class);
        HealthCheckRequest request = new HealthCheckRequest(1L, true);

        when(event.getData()).thenReturn(request);
        doThrow(new RuntimeException("expected")).when(freeIpaServiceStartService).pollFreeIpaHealth(any());

        assertTrue(underTest.doAccept(event) instanceof HealthCheckFailed);
    }
}