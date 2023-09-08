package com.sequenceiq.cloudbreak.quartz.configuration;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@ConditionalOnProperty(value = "quartz.common.scheduler.enabled", matchIfMissing = true)
@Configuration
public class SchedulerFactoryConfig {

    public static final String QUARTZ_EXECUTOR_THREAD_NAME_PREFIX = "quartzExecutor-";

    public static final String METERING_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX = "meteringQuartzExecutor-";

    private static final String QUARTZ_TASK_EXECUTOR = "quartzTaskExecutor";

    @Value("${quartz.threadpool.core.size:10}")
    private int quartzCorePoolSize;

    @Value("${quartz.threadpool.max.size:10}")
    private int quartzMaxPoolSize;

    @Value("${quartz.threadpool.capacity.size:-1}")
    private int quartzQueueCapacity;

    @Value("${quartz.threadpool.thread.priority:5}")
    private int quartzThreadPriority;

    @Value("${quartz.threadpool.custom.executor:true}")
    private boolean customExecutorEnabled;

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

    @Primary
    @Bean
    public SchedulerFactoryBean quartzScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        return SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
    }

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setAutoStartup(properties.isAutoSyncEnabled());
            bean.setGlobalJobListeners(resourceCheckerJobListener, jobMetricsListener);
            bean.setGlobalTriggerListeners(triggerMetricsListener);
            bean.setSchedulerListeners(schedulerMetricsListener);
            if (customExecutorEnabled) {
                bean.setTaskExecutor(quartzTaskExecutor());
            }
        };
    }

    @Bean(name = QUARTZ_TASK_EXECUTOR)
    public ExecutorService quartzTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(quartzCorePoolSize);
        executor.setMaxPoolSize(quartzMaxPoolSize);
        executor.setQueueCapacity(quartzQueueCapacity == -1 ? Integer.MAX_VALUE : quartzQueueCapacity);
        executor.setThreadPriority(quartzThreadPriority);
        executor.setThreadNamePrefix(QUARTZ_EXECUTOR_THREAD_NAME_PREFIX);
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), QUARTZ_TASK_EXECUTOR, "threadpool");
    }
}
