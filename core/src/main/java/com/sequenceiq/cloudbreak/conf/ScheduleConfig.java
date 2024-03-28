package com.sequenceiq.cloudbreak.conf;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerDecorator;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerThreadPoolTaskScheduler;
import com.sequenceiq.cloudbreak.concurrent.TaskCounterDecorator;
import com.sequenceiq.cloudbreak.concurrent.TimeTaskDecorator;

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

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Bean(name = SCHEDULER_TASK_EXECUTOR)
    public TaskScheduler taskScheduler() {
        if (virtualThreadsAvailable) {
            SimpleAsyncTaskScheduler taskScheduler = new SimpleAsyncTaskScheduler();
            taskScheduler.setThreadNamePrefix("scheduledExecutor-");
            taskScheduler.setVirtualThreads(true);
            TimeTaskDecorator timeTaskDecorator = new TimeTaskDecorator(meterRegistry, SCHEDULER_TASK_EXECUTOR);
            TaskCounterDecorator taskCounterDecorator = new TaskCounterDecorator();
            commonExecutorServiceFactory.monitorTaskCount(taskCounterDecorator, SCHEDULER_TASK_EXECUTOR);
            taskScheduler.setTaskDecorator(new CompositeTaskDecorator(List.of(new MDCCleanerDecorator(), timeTaskDecorator, taskCounterDecorator)));
            return taskScheduler;
        } else {
            ThreadPoolTaskScheduler threadPoolTaskScheduler = new MDCCleanerThreadPoolTaskScheduler();
            threadPoolTaskScheduler.setPoolSize(taskSchedulerPoolSize);
            threadPoolTaskScheduler.setThreadNamePrefix("scheduledExecutor-");
            threadPoolTaskScheduler.initialize();
            ExecutorServiceMetrics.monitor(meterRegistry, threadPoolTaskScheduler.getScheduledExecutor(), SCHEDULER_TASK_EXECUTOR, "threadpool");
            return threadPoolTaskScheduler;
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}
