package com.sequenceiq.cloudbreak.core.flow2;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent;

public class EventConverterAdapterTest {
    private EventConverterAdapter<StackStopEvent> eventConverter = new EventConverterAdapter<>(StackStopEvent.class);

    @Test
    public void convertTest() {
        StackStopEvent event = eventConverter.convert("STOPSTACKFINALIZED");
        Assert.assertEquals(StackStopEvent.STOP_FINALIZED_EVENT, event);
    }
}
