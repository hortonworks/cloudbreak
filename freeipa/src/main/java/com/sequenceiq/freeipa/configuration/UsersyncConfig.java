package com.sequenceiq.freeipa.configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.sequenceiq.cloudbreak.concurrent.ActorCrnTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.concurrent.CompositeTaskDecorator;
import com.sequenceiq.cloudbreak.concurrent.MDCCopyDecorator;
import com.sequenceiq.cloudbreak.concurrent.TimeTaskDecorator;

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

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Bean(name = USERSYNC_EXTERNAL_TASK_EXECUTOR)
    public ExecutorService usersyncExternalTaskExecutor() {
        if (virtualThreadsAvailable) {
            return commonExecutorServiceFactory.newVirtualThreadExecutorService("usersyncExecutor-external", USERSYNC_EXTERNAL_TASK_EXECUTOR,
                    List.of(new MDCCopyDecorator(), new ActorCrnTaskDecorator(),
                            new TimeTaskDecorator(meterRegistry, USERSYNC_EXTERNAL_TASK_EXECUTOR)));
        } else {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(usersyncCorePoolSize);
            executor.setQueueCapacity(usersyncQueueCapacity);
            executor.setThreadNamePrefix("usersyncExecutor-external-");
            List<TaskDecorator> decorators = List.of(new MDCCopyDecorator(), new ActorCrnTaskDecorator());
            executor.setTaskDecorator(new CompositeTaskDecorator(decorators));
            executor.initialize();
            return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), USERSYNC_EXTERNAL_TASK_EXECUTOR, "threadpool");
        }
    }

    @Bean(name = USERSYNC_INTERNAL_TASK_EXECUTOR)
    public ExecutorService usersyncInternalTaskExecutor() {
        if (virtualThreadsAvailable) {
            return commonExecutorServiceFactory.newVirtualThreadExecutorService("usersyncExecutor-internal", USERSYNC_INTERNAL_TASK_EXECUTOR,
                    List.of(new MDCCopyDecorator(), new ActorCrnTaskDecorator(),
                            new TimeTaskDecorator(meterRegistry, USERSYNC_INTERNAL_TASK_EXECUTOR)));
        } else {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(usersyncCorePoolSize);
            executor.setQueueCapacity(usersyncQueueCapacity);
            executor.setThreadNamePrefix("usersyncExecutor-internal-");
            executor.setTaskDecorator(
                    new CompositeTaskDecorator(
                            List.of(new MDCCopyDecorator(), new ActorCrnTaskDecorator())));
            executor.initialize();
            return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), USERSYNC_INTERNAL_TASK_EXECUTOR, "threadpool");
        }
    }

    @Bean(name = USERSYNC_TIMEOUT_TASK_EXECUTOR)
    public ScheduledExecutorService usersyncScheduledTaskExecutor() {
        if (virtualThreadsAvailable) {
            return Executors.newScheduledThreadPool(Integer.MAX_VALUE,
                    Thread.ofVirtual().inheritInheritableThreadLocals(true).name("usersyncExecutor-timeout-", 0).factory());
        } else {
            ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
            executor.setPoolSize(usersyncCorePoolSize);
            executor.setThreadNamePrefix("usersyncExecutor-timeout-");
            executor.initialize();
            return ExecutorServiceMetrics.monitor(meterRegistry, executor.getScheduledExecutor(), USERSYNC_TIMEOUT_TASK_EXECUTOR, "threadpool");
        }
    }
}
