package com.sequenceiq.flow.reactor.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Callable;
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
import com.sequenceiq.flow.reactor.api.event.DelayEvent;
import com.sequenceiq.flow.reactor.api.event.DelayFailedEvent;

@ExtendWith(MockitoExtension.class)
class DelayHandlerTest {
    private static final long RESOURCE_ID = 123L;

    private static final long DELAY = 10L;

    @Mock
    private Optional<DelayedExecutorService> delayedExecutorService;

    @Mock
    private DelayedExecutorService executorService;

    @InjectMocks
    private DelayHandler delayHandler;

    @Test
    void testDoAcceptWithDelayedExecutorService() throws ExecutionException, InterruptedException {
        Selectable successEvent = mock(Selectable.class);
        DelayEvent delayEvent = new DelayEvent(RESOURCE_ID, successEvent, DELAY, true);
        when(delayedExecutorService.isPresent()).thenReturn(Boolean.TRUE);
        when(delayedExecutorService.get()).thenReturn(executorService);
        when(executorService.runWithDelay(any(Callable.class), eq(DELAY), eq(TimeUnit.SECONDS)))
                .thenReturn(delayEvent.successEvent());

        Selectable result = delayHandler.doAccept(new HandlerEvent<>(new Event<>(delayEvent)));

        verify(executorService, times(1)).runWithDelay(any(Callable.class), eq(DELAY), eq(TimeUnit.SECONDS));
        assertEquals(delayEvent.successEvent(), result);
    }

    @Test
    void testDoAcceptWithoutDelayedExecutorService() {
        Selectable successEvent = mock(Selectable.class);
        DelayEvent delayEvent = new DelayEvent(RESOURCE_ID, successEvent, DELAY, true);

        Selectable result = delayHandler.doAccept(new HandlerEvent<>(new Event<>(delayEvent)));

        assertEquals(delayEvent.successEvent(), result);
    }

    @Test
    void testDoAcceptWithExecutionException() throws ExecutionException, InterruptedException {
        Selectable successEvent = mock(Selectable.class);
        DelayEvent delayEvent = new DelayEvent(RESOURCE_ID, successEvent, DELAY, false);

        ExecutionException exception = new ExecutionException("Execution exception", new RuntimeException());
        when(delayedExecutorService.isPresent()).thenReturn(Boolean.TRUE);
        when(delayedExecutorService.get()).thenReturn(executorService);
        when(executorService.runWithDelay(any(), anyLong(), any(TimeUnit.class)))
                .thenThrow(exception);

        Selectable result = delayHandler.doAccept(new HandlerEvent<>(new Event<>(delayEvent)));

        verify(executorService, times(1)).runWithDelay(any(), anyLong(), any(TimeUnit.class));
        assertEquals(new DelayFailedEvent(delayEvent.getResourceId(), exception), result);
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable successEvent = mock(Selectable.class);
        DelayEvent delayEvent = new DelayEvent(RESOURCE_ID, successEvent, DELAY, false);
        Exception exception = new RuntimeException("Test exception");

        Selectable result = delayHandler.defaultFailureEvent(delayEvent.getResourceId(), exception, new Event<>(delayEvent));

        assertEquals(new DelayFailedEvent(delayEvent.getResourceId(), exception), result);
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(DelayEvent.class), delayHandler.selector());
    }

}