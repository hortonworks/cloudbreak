package com.sequenceiq.freeipa.configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.sequenceiq.cloudbreak.concurrent.ActorCrnTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.CompositeTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.MdcCopyingTaskDecorator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
public class UsersyncConfig {

    public static final String USERSYNC_EXTERNAL_TASK_EXECUTOR = "usersyncExternalTaskExecutor";

    public static final String USERSYNC_TIMEOUT_TASK_EXECUTOR = "usersyncScheduledTaskExecutor";

    public static final String USERSYNC_INTERNAL_TASK_EXECUTOR = "usersyncInternalTaskExecutor";

    @Value("${freeipa.usersync.threadpool.core.size}")
    private int usersyncCorePoolSize;

    @Value("${freeipa.usersync.threadpool.capacity.size}")
    private int usersyncQueueCapacity;

    @Inject
    private MeterRegistry meterRegistry;

    @Bean(name = USERSYNC_EXTERNAL_TASK_EXECUTOR)
    public ExecutorService usersyncExternalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(usersyncCorePoolSize);
        executor.setQueueCapacity(usersyncQueueCapacity);
        executor.setThreadNamePrefix("usersyncExecutor-external-");
        executor.setTaskDecorator(
                new CompositeTaskDecorator(
                        List.of(new MdcCopyingTaskDecorator(), new ActorCrnTaskDecorator())));
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), USERSYNC_EXTERNAL_TASK_EXECUTOR, "threadpool");
    }

    @Bean(name = USERSYNC_INTERNAL_TASK_EXECUTOR)
    public ExecutorService usersyncInternalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(usersyncCorePoolSize);
        executor.setQueueCapacity(usersyncQueueCapacity);
        executor.setThreadNamePrefix("usersyncExecutor-internal-");
        executor.setTaskDecorator(
                new CompositeTaskDecorator(
                        List.of(new MdcCopyingTaskDecorator(), new ActorCrnTaskDecorator())));
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), USERSYNC_INTERNAL_TASK_EXECUTOR, "threadpool");
    }

    @Bean(name = USERSYNC_TIMEOUT_TASK_EXECUTOR)
    public ScheduledExecutorService usersyncScheduledTaskExecutor() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(usersyncCorePoolSize);
        executor.setThreadNamePrefix("usersyncExecutor-timeout-");
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getScheduledExecutor(), USERSYNC_TIMEOUT_TASK_EXECUTOR, "threadpool");
    }
}
