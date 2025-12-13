package com.sequenceiq.flow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EventConverterAdapterTest {
    private final EventConverterAdapter<TestEvent> eventConverter = new EventConverterAdapter<>(TestEvent.class);

    @Test
    void convertTest() {
        TestEvent event = eventConverter.convert("TestEvent");
        assertEquals(TestEvent.TEST_EVENT, event);
    }

    @Test
    void convertTestWhenUnknownEvent() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> eventConverter.convert("UnknownEvent"));
        assertEquals("Cannot convert class com.sequenceiq.flow.core.EventConverterAdapterTest$TestEvent enum type to UnknownEvent key," +
                " most probably this is an event from a different flow!", exception.getMessage());
    }

    private enum TestEvent implements FlowEvent {
        TEST_EVENT("TestEvent");

        private final String event;

        TestEvent(String event) {
            this.event = event;
        }

        @Override
        public String event() {
            return event;
        }
    }
}
