package com.sequenceiq.cloudbreak.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MeasureAspectsTest.TestConfig.class)
class MeasureAspectsTest {

    @Autowired
    private MeasuredService measuredService;

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(MeasuredService.class);
        logger.setLevel(Level.DEBUG);
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(MeasuredService.class)).detachAppender(logAppender);
    }

    @Test
    void logsTimingAndReturnsTheResultWhenTheMethodSucceeds() {
        String result = measuredService.succeed();

        assertEquals("ok", result);
        assertEquals(1, measurements("succeed").size());
    }

    @Test
    void logsTimingAndRethrowsTheSameExceptionWhenTheMethodFails() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> measuredService.fail());

        assertSame(MeasuredService.FAILURE, thrown);
        assertEquals(1, measurements("fail").size());
    }

    @Test
    void logsTimingForCheckedExceptionsAndKeepsThemUnchanged() {
        assertThrows(IOException.class, () -> measuredService.failChecked());

        assertEquals(1, measurements("failChecked").size());
    }

    @Test
    void doesNotLogForMethodsWithoutTheAnnotation() {
        measuredService.notMeasured();

        assertTrue(logAppender.list.stream().noneMatch(event -> event.getFormattedMessage().contains("notMeasured")));
    }

    private List<String> measurements(String method) {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("Measuring method execution: MeasuredService." + method + " took"))
                .collect(Collectors.toList());
    }

    @Component
    static class MeasuredService {

        static final IllegalStateException FAILURE = new IllegalStateException("boom");

        @Measure(MeasuredService.class)
        public String succeed() {
            return "ok";
        }

        @Measure(MeasuredService.class)
        public String fail() {
            throw FAILURE;
        }

        @Measure(MeasuredService.class)
        public String failChecked() throws IOException {
            throw new IOException("io");
        }

        public String notMeasured() {
            return "plain";
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import({MeasureAspects.class, MeasuredService.class})
    static class TestConfig {
    }
}
