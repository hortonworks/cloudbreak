package com.sequenceiq.flow.reactor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.config.EventBusConfig;

class ContextClosedEventHandlerTest {

    @Test
    public void tetsHandleContextClosedEventShouldStopExecutor() {
        ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(EventBusConfig.class, ContextClosedEventHandler.class,
                FlowRegister.class, TestMetricService.class);

        MDCCleanerThreadPoolExecutor eventBusThreadPoolExecutor = applicationContext.getBean("eventBusThreadPoolExecutor", MDCCleanerThreadPoolExecutor.class);
        Assertions.assertFalse(eventBusThreadPoolExecutor.isShutdown());

        applicationContext.close();

        Assertions.assertTrue(eventBusThreadPoolExecutor.isShutdown());
    }

    @TestComponent
    public static class TestMetricService extends AbstractMetricService {

        @Override
        protected String getMetricPrefix() {
            return "test";
        }
    }

}