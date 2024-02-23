package com.sequenceiq.cloudbreak.job.dynamicentitlement.scheduler;

import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.METRIC_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.QUARTZ_DYNAMIC_ENTITLEMENT_EXECUTOR_THREAD_NAME_PREFIX;

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

@ConditionalOnProperty(value = "quartz.dynamic-entitlement.scheduler.enabled", matchIfMissing = true)
@Configuration
public class DynamicEntitlementRefreshSchedulerFactoryConfig {

    private static final String QUARTZ_DYNAMIC_ENTITLEMENT_TASK_EXECUTOR = "quartzDynamicEntitlementTaskExecutor";

    private static final String DYNAMIC_ENTITLEMENT_SCHEDULER = "dynamicEntitlementScheduler";

    @Value("${quartz.dynamic-entitlement.common.threadpool.size:15}")
    private int threadpoolSize;

    @Value("${quartz.dynamic-entitlement.common.threadpool.priority:5}")
    private int threadpoolPriority;

    @Value("${quartz.dynamic-entitlement.common.threadpool.custom.executor:true}")
    private boolean customExecutorEnabled;

    @Inject
    private MeterRegistry meterRegistry;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Inject
    private ResourceCheckerJobListener resourceCheckerJobListener;

    @Bean
    public SchedulerFactoryBean dynamicEntitlementScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
        dynamicEntitlementSchedulerFactoryBeanCustomizer().customize(schedulerFactoryBean);
        return schedulerFactoryBean;
    }

    private SchedulerFactoryBeanCustomizer dynamicEntitlementSchedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setSchedulerName(DYNAMIC_ENTITLEMENT_SCHEDULER);
            if (customExecutorEnabled) {
                bean.setTaskExecutor(quartzDynamicEntitlementTaskExecutor());
            }
            bean.setGlobalJobListeners(resourceCheckerJobListener, new JobMetricsListener(metricService, DYNAMIC_ENTITLEMENT_SCHEDULER));
            bean.setGlobalTriggerListeners(new TriggerMetricsListener(metricService, DYNAMIC_ENTITLEMENT_SCHEDULER));
            bean.setSchedulerListeners(new SchedulerMetricsListener(metricService, DYNAMIC_ENTITLEMENT_SCHEDULER));
        };
    }

    @Bean(name = QUARTZ_DYNAMIC_ENTITLEMENT_TASK_EXECUTOR)
    public Executor quartzDynamicEntitlementTaskExecutor() {
        SimpleThreadPoolTaskExecutor executor = new SimpleThreadPoolTaskExecutor();
        executor.setThreadPriority(threadpoolPriority);
        executor.setThreadCount(threadpoolSize);
        executor.setThreadNamePrefix(QUARTZ_DYNAMIC_ENTITLEMENT_EXECUTOR_THREAD_NAME_PREFIX);
        return new TimedSimpleThreadPoolTaskExecutor(meterRegistry, executor, QUARTZ_DYNAMIC_ENTITLEMENT_TASK_EXECUTOR, METRIC_PREFIX, Set.of());
    }
}
