package com.sequenceiq.cloudbreak.core.flow2;

import org.junit.Assert;
import org.junit.Test;

public class EventConverterAdapterTest {
    private final EventConverterAdapter<TestEvent> eventConverter = new EventConverterAdapter<>(TestEvent.class);

    @Test
    public void convertTest() {
        TestEvent event = eventConverter.convert("TestEvent");
        Assert.assertEquals(TestEvent.TEST_EVENT, event);
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
