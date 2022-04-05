package com.sequenceiq.cloudbreak.quartz.configuration;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
public class SchedulerFactoryConfig {

    private static final String QUARTZ_TASK_EXECUTOR = "quartzTaskExecutor";

    @Value("${quartz.threadpool.core.size:10}")
    private int quartzCorePoolSize;

    @Value("${quartz.threadpool.max.size:10}")
    private int quartzMaxPoolSize;

    @Value("${quartz.threadpool.capacity.size:0}")
    private int quartzQueueCapacity;

    @Value("${quartz.threadpool.thread.priority:5}")
    private int quartzThreadPriority;

    @Inject
    private StatusCheckerConfig properties;

    @Inject
    private JobMetricsListener jobMetricsListener;

    @Inject
    private TriggerMetricsListener triggerMetricsListener;

    @Inject
    private ResourceCheckerJobListener resourceCheckerJobListener;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private SchedulerMetricsListener schedulerMetricsListener;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setAutoStartup(properties.isAutoSyncEnabled());
            bean.setGlobalJobListeners(resourceCheckerJobListener, jobMetricsListener);
            bean.setGlobalTriggerListeners(triggerMetricsListener);
            bean.setSchedulerListeners(schedulerMetricsListener);
            bean.setTaskExecutor(quartzTaskExecutor());
        };
    }

    @Bean(name = QUARTZ_TASK_EXECUTOR)
    public ExecutorService quartzTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(quartzCorePoolSize);
        executor.setMaxPoolSize(quartzMaxPoolSize);
        executor.setQueueCapacity(quartzQueueCapacity);
        executor.setThreadPriority(quartzThreadPriority);
        executor.setThreadNamePrefix("quartzExecutor-");
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), QUARTZ_TASK_EXECUTOR, "threadpool");
    }

}
