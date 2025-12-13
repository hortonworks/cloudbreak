package com.sequenceiq.flow.reactor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.config.EventBusConfig;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ContextClosedEventHandlerTest {

    public static final String TEST_PROFILE = "test";

    @Test
    public void testHandleContextClosedEventShouldStopExecutor() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.getEnvironment().setActiveProfiles(TEST_PROFILE);
        applicationContext.register(EventBusConfig.class, ContextClosedEventHandler.class,
                FlowRegister.class, TestMetricService.class, CommonExecutorServiceFactory.class, TestMeterRegistry.class);
        applicationContext.refresh();

        ExecutorService eventBusThreadPoolExecutor = applicationContext.getBean("eventBusThreadPoolExecutor", ExecutorService.class);
        assertFalse(eventBusThreadPoolExecutor.isShutdown());

        applicationContext.close();

        assertTrue(eventBusThreadPoolExecutor.isShutdown());
    }

    @TestComponent
    @Profile(TEST_PROFILE)
    @Qualifier("CommonMetricService")
    static class TestMetricService extends AbstractMetricService {

        @Override
        protected Optional<String> getMetricPrefix() {
            return Optional.of("test");
        }
    }

    @TestComponent
    @Profile(TEST_PROFILE)
    static class TestMeterRegistry extends SimpleMeterRegistry {
    }

}