package com.sequenceiq.cloudbreak.conf;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.sequenceiq.cloudbreak.concurrent.MDCCleanerThreadPoolTaskScheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
@EnableScheduling
public class ScheduleConfig implements SchedulingConfigurer {

    private static final String SCHEDULER_TASK_EXECUTOR = "schedulerTaskExecutor";

    @Value("${cb.task.scheduler.pool.size:10}")
    private int taskSchedulerPoolSize;

    @Inject
    private MeterRegistry meterRegistry;

    @Bean(name = SCHEDULER_TASK_EXECUTOR)
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new MDCCleanerThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(taskSchedulerPoolSize);
        threadPoolTaskScheduler.setThreadNamePrefix("scheduledExecutor-");
        return threadPoolTaskScheduler;
    }

    @Bean
    public ExecutorService taskSchedulerExecutorService(@Qualifier(SCHEDULER_TASK_EXECUTOR) ThreadPoolTaskScheduler taskScheduler) {
        return ExecutorServiceMetrics.monitor(meterRegistry, taskScheduler.getScheduledExecutor(), SCHEDULER_TASK_EXECUTOR, "threadpool");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}
