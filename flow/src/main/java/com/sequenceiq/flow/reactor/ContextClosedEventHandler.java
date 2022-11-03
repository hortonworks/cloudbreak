package com.sequenceiq.flow.reactor;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.flow.core.FlowRegister;

@Component
public class ContextClosedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextClosedEventHandler.class);

    @Value("${cb.eventbus.threadpool.shutdown.timeout.seconds:15}")
    private long eventBusThreadpoolShutdownTimeout;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    @Named("eventBusThreadPoolExecutor")
    private MDCCleanerThreadPoolExecutor executor;

    @EventListener
    public void handleContextClosedEvent(ContextClosedEvent event) {
        LOGGER.info("ContextClosedEvent received, shutdown eventBusThreadPoolExecutor. Running flows: {}", flowRegister.getRunningFlowIds());
        shutdownEventBusThreadPoolExecutor();
    }

    private void shutdownEventBusThreadPoolExecutor() {
        LOGGER.debug("Shutting down executor service.");
        executor.shutdownNow();
        LOGGER.info("Executor service has been shut down.");
        try {
            if (!executor.awaitTermination(eventBusThreadpoolShutdownTimeout, TimeUnit.SECONDS)) {
                LOGGER.warn("eventBusThreadPoolExecutor shutdown timed out.");
            }
        } catch (InterruptedException e) {
            LOGGER.warn("eventBusThreadPoolExecutor shutdown is interrupted.", e);
            Thread.currentThread().interrupt();
        }
    }

}