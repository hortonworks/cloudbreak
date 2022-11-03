package com.sequenceiq.cloudbreak.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventBusTest {

    @Mock
    private EventRouter eventRouter;

    @Mock
    private Executor executor;

    private EventBus underTest;

    @BeforeEach
    public void setUp() {
        underTest = new EventBus(eventRouter, executor);
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(executor).execute(any());
    }

    @Test
    void testNotify() {
        Event<String> data = Event.wrap("Data");
        underTest.notify("KEY", data);

        assertEquals("KEY", data.getKey());
        verify(executor).execute(any());
        verify(eventRouter).handle(data);
    }

    @Test
    void testAddHandler() {
        Consumer<Event<?>> consumer = e -> {
        };
        underTest.on("KEY", consumer);

        verify(eventRouter).addHandler("KEY", consumer);
    }

}