package com.sequenceiq.cloudbreak.reactor.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.bus.Event;
import reactor.bus.EventBus;

@RunWith(MockitoJUnitRunner.class)
public class ExceptionCatcherEventHandlerTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String SELECTOR = "Selector";

    @InjectMocks
    private ExceptionCatcherEventHandlerTestHelper underTest;

    @Mock
    private EventBus eventBus;

    @Test
    public void testAcceptWhenNoEventNotification() {
        Event<Payload> event = mock(Event.class);
        Event.Headers headers = mock(Event.Headers.class);
        Payload payload = mock(Payload.class);
        Selectable failureEvent = mock(Selectable.class);
        underTest.initialize(SELECTOR, failureEvent, false, null);

        when(event.getData()).thenReturn(payload);
        when(payload.getResourceId()).thenReturn(RESOURCE_ID);
        when(event.getHeaders()).thenReturn(headers);
        when(failureEvent.selector()).thenReturn(SELECTOR);


        underTest.accept(event);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(SELECTOR), eventCaptor.capture());
        Event eventBusEvent = eventCaptor.getValue();
        assertNotNull(eventBusEvent);
        assertEquals(failureEvent, eventBusEvent.getData());
    }

    @Test
    public void testAcceptWhenException() {
        Event<Payload> event = mock(Event.class);
        Event.Headers headers = mock(Event.Headers.class);
        Payload payload = mock(Payload.class);
        Selectable failureEvent = mock(Selectable.class);
        underTest.initialize(SELECTOR, failureEvent, true, null);

        when(event.getData()).thenReturn(payload);
        when(payload.getResourceId()).thenReturn(RESOURCE_ID);
        when(event.getHeaders()).thenReturn(headers);
        when(failureEvent.selector()).thenReturn(SELECTOR);

        underTest.accept(event);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(SELECTOR), eventCaptor.capture());
        Event eventBusEvent = eventCaptor.getValue();
        assertNotNull(eventBusEvent);
        assertEquals(failureEvent, eventBusEvent.getData());
    }

    @Test
    public void testAcceptWhenSuccessful() {
        Event<Payload> event = mock(Event.class);
        Event.Headers headers = mock(Event.Headers.class);
        Selectable sendEventObject = mock(Selectable.class);
        underTest.initialize(SELECTOR, null, false, sendEventObject);

        when(event.getHeaders()).thenReturn(headers);
        when(sendEventObject.selector()).thenReturn(SELECTOR);

        underTest.accept(event);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(SELECTOR), eventCaptor.capture());
        Event eventBusEvent = eventCaptor.getValue();
        assertNotNull(eventBusEvent);
        assertEquals(sendEventObject, eventBusEvent.getData());
    }
}
