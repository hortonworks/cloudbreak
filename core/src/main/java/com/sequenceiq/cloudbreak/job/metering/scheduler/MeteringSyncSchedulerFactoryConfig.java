package com.sequenceiq.cloudbreak.job.metering.scheduler;

import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.EXECUTOR_THREAD_NAME_POSTFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.METRIC_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.SCHEDULER_POSTFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.TASK_EXECUTOR_POSTFIX;

import java.util.Set;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryBeanUtil;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TimedSimpleThreadPoolTaskExecutor;
import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;

import io.micrometer.core.instrument.MeterRegistry;

@ConditionalOnProperty(value = "quartz.metering.sync.scheduler.enabled", matchIfMissing = true)
@Configuration
public class MeteringSyncSchedulerFactoryConfig {

    public static final String QUARTZ_METERING_SYNC_PREFIX = "quartzMeteringSync";

    public static final String QUARTZ_METERING_SYNC_SCHEDULER = QUARTZ_METERING_SYNC_PREFIX + SCHEDULER_POSTFIX;

    private static final String QUARTZ_METERING_SYNC_EXECUTOR_THREAD_NAME_PREFIX = QUARTZ_METERING_SYNC_PREFIX + EXECUTOR_THREAD_NAME_POSTFIX;

    private static final String QUARTZ_METERING_SYNC_TASK_EXECUTOR = QUARTZ_METERING_SYNC_PREFIX + TASK_EXECUTOR_POSTFIX;

    @Value("${quartz.metering.sync.threadpool.size:15}")
    private int threadpoolSize;

    @Value("${quartz.metering.sync.threadpool.priority:5}")
    private int threadpoolPriority;

    @Value("${quartz.metering.sync.threadpool.custom.executor:true}")
    private boolean customExecutorEnabled;

    @Inject
    private MeterRegistry meterRegistry;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Inject
    private ResourceCheckerJobListener resourceCheckerJobListener;

    @Bean(name = QUARTZ_METERING_SYNC_SCHEDULER)
    public SchedulerFactoryBean quartzMeteringSyncScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
        meteringSyncSchedulerFactoryBeanCustomizer().customize(schedulerFactoryBean);
        return schedulerFactoryBean;
    }

    private SchedulerFactoryBeanCustomizer meteringSyncSchedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setSchedulerName(QUARTZ_METERING_SYNC_SCHEDULER);
            if (customExecutorEnabled) {
                bean.setTaskExecutor(quartzMeteringSyncTaskExecutor());
            }
            bean.setGlobalJobListeners(resourceCheckerJobListener, new JobMetricsListener(metricService, QUARTZ_METERING_SYNC_SCHEDULER));
            bean.setGlobalTriggerListeners(new TriggerMetricsListener(metricService, QUARTZ_METERING_SYNC_SCHEDULER));
            bean.setSchedulerListeners(new SchedulerMetricsListener(metricService, QUARTZ_METERING_SYNC_SCHEDULER));
        };
    }

    @Bean(name = QUARTZ_METERING_SYNC_TASK_EXECUTOR)
    public Executor quartzMeteringSyncTaskExecutor() {
        SimpleThreadPoolTaskExecutor executor = new SimpleThreadPoolTaskExecutor();
        executor.setThreadPriority(threadpoolPriority);
        executor.setThreadCount(threadpoolSize);
        executor.setThreadNamePrefix(QUARTZ_METERING_SYNC_EXECUTOR_THREAD_NAME_PREFIX);
        return new TimedSimpleThreadPoolTaskExecutor(meterRegistry, executor, QUARTZ_METERING_SYNC_TASK_EXECUTOR, METRIC_PREFIX, Set.of());
    }
}
