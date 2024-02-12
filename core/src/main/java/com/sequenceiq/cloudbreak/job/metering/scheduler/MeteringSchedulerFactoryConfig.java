package com.sequenceiq.cloudbreak.job.metering.scheduler;

import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.METRIC_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX;

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
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryBeanUtil;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TimedSimpleThreadPoolTaskExecutor;

import io.micrometer.core.instrument.MeterRegistry;

@ConditionalOnProperty(value = "quartz.metering.common.scheduler.enabled", matchIfMissing = true)
@Configuration
public class MeteringSchedulerFactoryConfig {

    private static final String QUARTZ_METERING_TASK_EXECUTOR = "quartzMeteringTaskExecutor";

    private static final String METERING_SCHEDULER = "meteringScheduler";

    @Value("${quartz.metering.common.threadpool.size:15}")
    private int threadpoolSize;

    @Value("${quartz.metering.common.threadpool.priority:5}")
    private int threadpoolPriority;

    @Value("${quartz.metering.common.threadpool.custom.executor:true}")
    private boolean customExecutorEnabled;

    @Inject
    private MeterRegistry meterRegistry;

    @Bean
    public SchedulerFactoryBean meteringScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
        meteringSchedulerFactoryBeanCustomizer().customize(schedulerFactoryBean);
        return schedulerFactoryBean;
    }

    private SchedulerFactoryBeanCustomizer meteringSchedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setSchedulerName(METERING_SCHEDULER);
            if (customExecutorEnabled) {
                bean.setTaskExecutor(quartzMeteringTaskExecutor());
            }
        };
    }

    @Bean(name = QUARTZ_METERING_TASK_EXECUTOR)
    public Executor quartzMeteringTaskExecutor() {
        SimpleThreadPoolTaskExecutor executor = new SimpleThreadPoolTaskExecutor();
        executor.setThreadPriority(threadpoolPriority);
        executor.setThreadCount(threadpoolSize);
        executor.setThreadNamePrefix(QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX);
        return new TimedSimpleThreadPoolTaskExecutor(meterRegistry, executor, QUARTZ_METERING_TASK_EXECUTOR, METRIC_PREFIX, Set.of());
    }
}
