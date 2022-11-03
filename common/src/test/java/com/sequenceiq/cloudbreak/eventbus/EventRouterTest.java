package com.sequenceiq.cloudbreak.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRouterTest {

    @Mock
    private Consumer<Event<?>> unhandledEventHandler;

    @Mock
    private BiConsumer<Event<?>, Throwable> exceptionHandler;

    private EventRouter underTest;

    @Mock
    private Consumer<Event<String>> handler1;

    @Mock
    private Consumer<Event<String>> handler2;

    @BeforeEach
    public void setUp() {
        underTest = new EventRouter(unhandledEventHandler, exceptionHandler);
    }

    @Test
    public void testAddingHandlerForTheSameKeyThrowsException() {
        underTest.addHandler("KEY", new Handler());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> underTest.addHandler("KEY", new Handler()));

        assertEquals("Handler is already registered for KEY key.\n" +
                " First registered type: com.sequenceiq.cloudbreak.eventbus.EventRouterTest.Handler,\n" +
                " second type: com.sequenceiq.cloudbreak.eventbus.EventRouterTest.Handler.", exception.getMessage());
    }

    @Test
    void testAddingHandlersForTwoDifferentKeysIsAccepted() {
        underTest.addHandler("KEY1", handler1);
        underTest.addHandler("KEY2", handler2);
    }

    @Test
    void testAddingOneHandlerForTwoDifferentKeysIsAccepted() {
        underTest.addHandler("KEY1", handler1);
        underTest.addHandler("KEY2", handler1);
    }

    @Test
    void testEventRouting() {
        underTest.addHandler("KEY1", handler1);
        underTest.addHandler("KEY2", handler2);

        Event<String> event1 = Event.wrap("Data");
        event1.setKey("KEY1");
        Event<String> event2 = Event.wrap("Data");
        event2.setKey("KEY2");

        underTest.handle(event1);
        underTest.handle(event2);

        verify(handler1).accept(event1);
        verify(handler2).accept(event2);

        verify(handler1, times(0)).accept(event2);
        verify(handler2, times(0)).accept(event1);
        verifyNoInteractions(unhandledEventHandler, exceptionHandler);
    }

    @Test
    void testUnhandledEvent() {
        Event<String> event = Event.wrap("Data");
        event.setKey("KEY");

        underTest.handle(event);

        verify(unhandledEventHandler).accept(event);
        verifyNoInteractions(exceptionHandler);
    }

    @Test
    void testExceptionHandlerIsCalledWhenEventHandlerThrowsException() {
        RuntimeException exception = new RuntimeException("Bad");
        doThrow(exception).when(handler1).accept(any());
        underTest.addHandler("KEY", handler1);
        Event<String> event = Event.wrap("Data");
        event.setKey("KEY");

        underTest.handle(event);

        verify(exceptionHandler).accept(eq(event), eq(exception));
        verifyNoInteractions(unhandledEventHandler);
    }

    private static class Handler implements Consumer<Event<String>> {

        @Override
        public void accept(Event<String> event) {

        }
    }

}