package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FINISHED_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class CleanupFreeIpaHandlerTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private FreeIpaCleanupService freeIpaCleanupService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private CleanupFreeIpaHandler underTest;

    @Test
    public void testRecover() {
        Event<CleanupFreeIpaEvent> cleanupFreeIpaEvent = mock(Event.class);
        Set<String> hostNames = Set.of("asdfg");
        Set<String> ips = Set.of("1.1.1.1");
        when(cleanupFreeIpaEvent.getData()).thenReturn(new CleanupFreeIpaEvent(1L, hostNames, ips, true));
        Stack stack = new Stack();
        when(stackService.get(1L)).thenReturn(stack);

        underTest.accept(cleanupFreeIpaEvent);

        verify(freeIpaCleanupService).cleanupOnRecover(stack, hostNames, ips);
        verify(freeIpaCleanupService, never()).cleanupOnScale(any(Stack.class), anySet(), anySet());
        verify(eventBus).notify(eq(CLEANUP_FREEIPA_FINISHED_EVENT.event()), any(Event.class));
    }

    @Test
    public void testNotRecover() {
        Event<CleanupFreeIpaEvent> cleanupFreeIpaEvent = mock(Event.class);
        Set<String> hostNames = Set.of("asdfg");
        Set<String> ips = Set.of("1.1.1.1");
        when(cleanupFreeIpaEvent.getData()).thenReturn(new CleanupFreeIpaEvent(1L, hostNames, ips, false));
        Stack stack = new Stack();
        when(stackService.get(1L)).thenReturn(stack);

        underTest.accept(cleanupFreeIpaEvent);

        verify(freeIpaCleanupService).cleanupOnScale(stack, hostNames, ips);
        verify(freeIpaCleanupService, never()).cleanupOnRecover(any(Stack.class), anySet(), anySet());
        verify(eventBus).notify(eq(CLEANUP_FREEIPA_FINISHED_EVENT.event()), any(Event.class));
    }

    @Test
    public void testEventSentOnErrorNotRecover() {
        Event<CleanupFreeIpaEvent> cleanupFreeIpaEvent = mock(Event.class);
        when(cleanupFreeIpaEvent.getData()).thenReturn(new CleanupFreeIpaEvent(1L, Set.of(), Set.of(), false));
        when(stackService.get(1L)).thenReturn(new Stack());
        doThrow(new RuntimeException()).when(freeIpaCleanupService).cleanupOnScale(any(Stack.class), anySet(), anySet());

        underTest.accept(cleanupFreeIpaEvent);

        verify(eventBus).notify(eq(CLEANUP_FREEIPA_FINISHED_EVENT.event()), any(Event.class));
    }

    @Test
    public void testEventSentOnErrorRecover() {
        Event<CleanupFreeIpaEvent> cleanupFreeIpaEvent = mock(Event.class);
        when(cleanupFreeIpaEvent.getData()).thenReturn(new CleanupFreeIpaEvent(1L, Set.of(), Set.of(), true));
        when(stackService.get(1L)).thenReturn(new Stack());
        doThrow(new RuntimeException()).when(freeIpaCleanupService).cleanupOnRecover(any(Stack.class), anySet(), anySet());

        underTest.accept(cleanupFreeIpaEvent);

        verify(eventBus).notify(eq(CLEANUP_FREEIPA_FINISHED_EVENT.event()), any(Event.class));
    }
}