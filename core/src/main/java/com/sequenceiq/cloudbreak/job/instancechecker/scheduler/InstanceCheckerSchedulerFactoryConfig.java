package com.sequenceiq.cloudbreak.job.instancechecker.scheduler;

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

@ConditionalOnProperty(value = "quartz.instancechecker.scheduler.enabled", matchIfMissing = true)
@Configuration
public class InstanceCheckerSchedulerFactoryConfig {

    public static final String QUARTZ_INSTANCE_CHECKER_PREFIX = "quartzInstanceChecker";

    public static final String QUARTZ_INSTANCE_CHECKER_SCHEDULER = QUARTZ_INSTANCE_CHECKER_PREFIX + SCHEDULER_POSTFIX;

    private static final String QUARTZ_INSTANCE_CHECKER_EXECUTOR_THREAD_NAME_PREFIX = QUARTZ_INSTANCE_CHECKER_PREFIX + EXECUTOR_THREAD_NAME_POSTFIX;

    private static final String QUARTZ_INSTANCE_CHECKER_TASK_EXECUTOR = QUARTZ_INSTANCE_CHECKER_PREFIX + TASK_EXECUTOR_POSTFIX;

    @Value("${quartz.instancechecker.threadpool.size:15}")
    private int threadpoolSize;

    @Value("${quartz.instancechecker.virtual-threadpool.size:200}")
    private int virtualThreadpoolSize;

    @Value("${quartz.instancechecker.threadpool.priority:5}")
    private int threadpoolPriority;

    @Value("${quartz.instancechecker.threadpool.custom.executor:true}")
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

    @Bean(name = QUARTZ_INSTANCE_CHECKER_SCHEDULER)
    public SchedulerFactoryBean quartzMeteringScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
        instanceCheckerSchedulerFactoryBeanCustomizer().customize(schedulerFactoryBean);
        return schedulerFactoryBean;
    }

    private SchedulerFactoryBeanCustomizer instanceCheckerSchedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setSchedulerName(QUARTZ_INSTANCE_CHECKER_SCHEDULER);
            if (customExecutorEnabled) {
                bean.setTaskExecutor(quartzInstanceCheckerTaskExecutor());
            }
            bean.setGlobalJobListeners(resourceCheckerJobListener, new JobMetricsListener(metricService, QUARTZ_INSTANCE_CHECKER_SCHEDULER));
            bean.setGlobalTriggerListeners(new TriggerMetricsListener(metricService, QUARTZ_INSTANCE_CHECKER_SCHEDULER));
            bean.setSchedulerListeners(new SchedulerMetricsListener(metricService, QUARTZ_INSTANCE_CHECKER_SCHEDULER));
        };
    }

    @Bean(name = QUARTZ_INSTANCE_CHECKER_TASK_EXECUTOR)
    public Executor quartzInstanceCheckerTaskExecutor() {
        ThreadPool executor;
        if (virtualThreadsAvailable) {
            executor = new VirtualThreadPoolTaskExecutor(QUARTZ_INSTANCE_CHECKER_EXECUTOR_THREAD_NAME_PREFIX, virtualThreadpoolSize, true);
        } else {
            SimpleThreadPoolTaskExecutor simpleThreadPoolTaskExecutor = new SimpleThreadPoolTaskExecutor();
            simpleThreadPoolTaskExecutor.setThreadPriority(threadpoolPriority);
            simpleThreadPoolTaskExecutor.setThreadCount(threadpoolSize);
            simpleThreadPoolTaskExecutor.setThreadNamePrefix(QUARTZ_INSTANCE_CHECKER_EXECUTOR_THREAD_NAME_PREFIX);
            executor = simpleThreadPoolTaskExecutor;
        }
        return new TimedSimpleThreadPoolTaskExecutor(meterRegistry, executor, QUARTZ_INSTANCE_CHECKER_TASK_EXECUTOR, METRIC_PREFIX, Set.of());
    }
}
