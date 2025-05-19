package com.sequenceiq.freeipa.configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.concurrent.ActorCrnTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.concurrent.CompositeTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.ConcurrencyLimitDecorator;
import com.sequenceiq.cloudbreak.concurrent.MDCCopyDecorator;
import com.sequenceiq.cloudbreak.concurrent.TimeTaskDecorator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
public class HealthCheckConfig {

    public static final String HEALTH_CHECK_TASK_EXECUTOR = "healthCheckTaskExecutor";

    private static final String NAME_PREFIX = "healthCheckExecutor-";

    @Value("${freeipa.healthCheck.threadpool.core.size:5}")
    private int healthCheckCorePoolSize;

    @Value("${freeipa.healthCheck.threadpool.capacity.size:100}")
    private int healthCheckQueueCapacity;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Bean(name = HEALTH_CHECK_TASK_EXECUTOR)
    public ExecutorService healthCheckExternalTaskExecutor() {
        if (virtualThreadsAvailable) {
            return commonExecutorServiceFactory.newVirtualThreadExecutorService(NAME_PREFIX, HEALTH_CHECK_TASK_EXECUTOR,
                    List.of(new MDCCopyDecorator(),
                            new TimeTaskDecorator(meterRegistry, HEALTH_CHECK_TASK_EXECUTOR),
                            new ConcurrencyLimitDecorator(healthCheckCorePoolSize)));
        } else {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(healthCheckCorePoolSize);
            executor.setQueueCapacity(healthCheckQueueCapacity);
            executor.setThreadNamePrefix(NAME_PREFIX);
            List<TaskDecorator> decorators = List.of(new MDCCopyDecorator(), new ActorCrnTaskDecorator());
            executor.setTaskDecorator(new CompositeTaskDecorator(decorators));
            executor.initialize();
            return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), HEALTH_CHECK_TASK_EXECUTOR, "threadpool");
        }
    }
}
