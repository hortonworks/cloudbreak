package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import java.util.Set;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryBeanUtil;
import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

import io.micrometer.core.instrument.MeterRegistry;

@ConditionalOnProperty(value = "quartz.default.scheduler.enabled", matchIfMissing = true)
@Configuration
public class SchedulerFactoryConfig {

    public static final String METRIC_PREFIX = "threadpool.";

    public static final String QUARTZ_EXECUTOR_THREAD_NAME_PREFIX = "quartzExecutor";

    public static final String QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX = "quartzMeteringExecutor";

    public static final String QUARTZ_METERING_SYNC_EXECUTOR_THREAD_NAME_PREFIX = "quartzMeteringSyncExecutor";

    private static final String QUARTZ_TASK_EXECUTOR = "quartzTaskExecutor";

    @Value("${quartz.default.threadpool.size:15}")
    private int threadpoolSize;

    @Value("${quartz.default.threadpool.priority:5}")
    private int threadpoolPriority;

    @Value("${quartz.default.threadpool.custom.executor:true}")
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
    public Executor quartzTaskExecutor() {
        SimpleThreadPoolTaskExecutor executor = new SimpleThreadPoolTaskExecutor();
        executor.setThreadPriority(threadpoolPriority);
        executor.setThreadCount(threadpoolSize);
        executor.setThreadNamePrefix(QUARTZ_EXECUTOR_THREAD_NAME_PREFIX);
        return new TimedSimpleThreadPoolTaskExecutor(meterRegistry, executor, QUARTZ_TASK_EXECUTOR, METRIC_PREFIX, Set.of());
    }
}
