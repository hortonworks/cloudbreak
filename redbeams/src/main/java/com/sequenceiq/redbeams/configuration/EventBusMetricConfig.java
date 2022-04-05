package com.sequenceiq.redbeams.configuration;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
public class EventBusMetricConfig {

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    @Named("eventBusThreadPoolExecutor")
    private MDCCleanerThreadPoolExecutor executor;

    @Bean
    public ExecutorService eventBusThreadPoolExecutorService() {
        return ExecutorServiceMetrics.monitor(meterRegistry, executor, "eventBusThreadPoolExecutor", "threadpool");
    }
}