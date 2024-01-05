package com.sequenceiq.cloudbreak.logger.format;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 * See LoggerContext-related additions at https://github.com/spring-projects/spring-boot/issues/36177#issuecomment-1618823883
 */
class CustomJsonLayoutTest {

    private CustomJsonLayout underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomJsonLayout("context", 10);
    }

    @Test
    void testLogIsSplitted() {
        LoggingEvent loggingEvent = new LoggingEvent();
        LoggerContext lc = new LoggerContext();
        lc.setMDCAdapter(MDC.getMDCAdapter());
        loggingEvent.setLoggerContext(lc);
        String result = underTest.doLayout(loggingEvent, "LONG MESSAGE");
        assertTrue(result.contains("partial_message=true"));
    }

    @Test
    void testLogIsNotSplitted() {
        LoggingEvent loggingEvent = new LoggingEvent();
        LoggerContext lc = new LoggerContext();
        lc.setMDCAdapter(MDC.getMDCAdapter());
        loggingEvent.setLoggerContext(lc);
        String result = underTest.doLayout(loggingEvent, "MESSAGE");
        assertFalse(result.contains("partial_message=true"));
    }
}
