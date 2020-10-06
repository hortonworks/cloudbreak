package com.sequenceiq.cloudbreak.logger.format;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.spi.LoggingEvent;

public class CustomJsonLayoutTest {

    private CustomJsonLayout underTest;

    @Before
    public void setUp() {
        underTest = new CustomJsonLayout("context", 10);
    }

    @Test
    public void testLogIsSplitted() {
        LoggingEvent loggingEvent = new LoggingEvent();
        String result = underTest.doLayout(loggingEvent, "LONG MESSAGE");
        assertTrue(result.contains("partial_message=true"));
    }

    @Test
    public void testLogIsNotSplitted() {
        LoggingEvent loggingEvent = new LoggingEvent();
        String result = underTest.doLayout(loggingEvent, "MESSAGE");
        assertFalse(result.contains("partial_message=true"));
    }
}
