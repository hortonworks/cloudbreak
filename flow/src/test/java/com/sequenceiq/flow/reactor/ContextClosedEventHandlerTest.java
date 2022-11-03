package com.sequenceiq.flow.reactor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.config.EventBusConfig;

class ContextClosedEventHandlerTest {

    public static final String TEST_PROFILE = "test";

    @Test
    public void testHandleContextClosedEventShouldStopExecutor() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.getEnvironment().setActiveProfiles(TEST_PROFILE);
        applicationContext.register(EventBusConfig.class, ContextClosedEventHandler.class,
                FlowRegister.class, TestMetricService.class);
        applicationContext.refresh();

        MDCCleanerThreadPoolExecutor eventBusThreadPoolExecutor = applicationContext.getBean("eventBusThreadPoolExecutor", MDCCleanerThreadPoolExecutor.class);
        Assertions.assertFalse(eventBusThreadPoolExecutor.isShutdown());

        applicationContext.close();

        Assertions.assertTrue(eventBusThreadPoolExecutor.isShutdown());
    }

    @TestComponent
    @Profile(TEST_PROFILE)
    static class TestMetricService extends AbstractMetricService {

        @Override
        protected String getMetricPrefix() {
            return "test";
        }
    }

}