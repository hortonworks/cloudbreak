package com.sequenceiq.cloudbreak.conf;

import java.util.List;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerDecorator;
import com.sequenceiq.cloudbreak.concurrent.TaskCounterDecorator;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
public class CloudbreakExecutorServiceConfiguration {

    @Value("${cb.executorservice.pool.size:20}")
    private int executorServicePoolSize;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Inject
    private MeterRegistry meterRegistry;

    @Bean
    ExecutorService cloudbreakListeningScheduledExecutorService() {
        if (virtualThreadsAvailable) {
            return commonExecutorServiceFactory.newVirtualThreadExecutorService("cb", "cloudbreakListeningScheduledExecutorService",
                    List.of(new MDCCleanerDecorator(), new TaskCounterDecorator()));
        } else {
            return ExecutorServiceMetrics.monitor(meterRegistry, new MDCCleanerScheduledExecutor(executorServicePoolSize,
                    new ThreadFactoryBuilder().setNameFormat("cb-%d").build()), "cloudbreakListeningScheduledExecutorService", "threadpool");
        }
    }
}
