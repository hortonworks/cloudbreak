package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import org.junit.Assert;
import org.junit.Test;

public class StackStopEventConverterTest {
    private StackStopEventConverter converter = new StackStopEventConverter();

    @Test
    public void convertTest() {
        StackStopEvent event = converter.convert("STOPFAILHANDLED");
        Assert.assertEquals(StackStopEvent.STOP_FAIL_HANDLED_EVENT, event);
    }
}
