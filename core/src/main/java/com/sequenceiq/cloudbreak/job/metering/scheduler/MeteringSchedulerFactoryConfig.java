package com.sequenceiq.cloudbreak.job.metering.scheduler;

import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.EXECUTOR_THREAD_NAME_POSTFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.METRIC_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.SCHEDULER_POSTFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.TASK_EXECUTOR_POSTFIX;

import java.util.Set;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.quartz.spi.ThreadPool;
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
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.VirtualThreadPoolTaskExecutor;
import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;

import io.micrometer.core.instrument.MeterRegistry;

@ConditionalOnProperty(value = "quartz.metering.common.scheduler.enabled", matchIfMissing = true)
@Configuration
public class MeteringSchedulerFactoryConfig {

    public static final String QUARTZ_METERING_PREFIX = "quartzMetering";

    public static final String QUARTZ_METERING_SCHEDULER = QUARTZ_METERING_PREFIX + SCHEDULER_POSTFIX;

    private static final String QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX = QUARTZ_METERING_PREFIX + EXECUTOR_THREAD_NAME_POSTFIX;

    private static final String QUARTZ_METERING_TASK_EXECUTOR = QUARTZ_METERING_PREFIX + TASK_EXECUTOR_POSTFIX;

    @Value("${quartz.metering.common.threadpool.size:15}")
    private int threadpoolSize;

    @Value("${quartz.metering.common.virtual-threadpool.size:200}")
    private int virtualThreadpoolSize;

    @Value("${quartz.metering.common.threadpool.priority:5}")
    private int threadpoolPriority;

    @Value("${quartz.metering.common.threadpool.custom.executor:true}")
    private boolean customExecutorEnabled;

    @Inject
    private MeterRegistry meterRegistry;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Inject
    private ResourceCheckerJobListener resourceCheckerJobListener;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Bean(name = QUARTZ_METERING_SCHEDULER)
    public SchedulerFactoryBean quartzMeteringScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
        meteringSchedulerFactoryBeanCustomizer().customize(schedulerFactoryBean);
        return schedulerFactoryBean;
    }

    private SchedulerFactoryBeanCustomizer meteringSchedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setSchedulerName(QUARTZ_METERING_SCHEDULER);
            if (customExecutorEnabled) {
                bean.setTaskExecutor(quartzMeteringTaskExecutor());
            }
            bean.setGlobalJobListeners(resourceCheckerJobListener, new JobMetricsListener(metricService, QUARTZ_METERING_SCHEDULER));
            bean.setGlobalTriggerListeners(new TriggerMetricsListener(metricService, QUARTZ_METERING_SCHEDULER));
            bean.setSchedulerListeners(new SchedulerMetricsListener(metricService, QUARTZ_METERING_SCHEDULER));
        };
    }

    @Bean(name = QUARTZ_METERING_TASK_EXECUTOR)
    public Executor quartzMeteringTaskExecutor() {
        ThreadPool executor;
        if (virtualThreadsAvailable) {
            executor = new VirtualThreadPoolTaskExecutor(QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX, virtualThreadpoolSize, true);
        } else {
            SimpleThreadPoolTaskExecutor simpleThreadPoolTaskExecutor = new SimpleThreadPoolTaskExecutor();
            simpleThreadPoolTaskExecutor.setThreadPriority(threadpoolPriority);
            simpleThreadPoolTaskExecutor.setThreadCount(threadpoolSize);
            simpleThreadPoolTaskExecutor.setThreadNamePrefix(QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX);
            executor = simpleThreadPoolTaskExecutor;
        }
        return new TimedSimpleThreadPoolTaskExecutor(meterRegistry, executor, QUARTZ_METERING_TASK_EXECUTOR, METRIC_PREFIX, Set.of());
    }
}
