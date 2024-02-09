package com.sequenceiq.cloudbreak.job.metering;

import static com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryConfig.METERING_SYNC_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX;

import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryBeanUtil;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
public class MeteringSyncSchedulerFactoryConfig {

    private static final String METERING_SYNC_QUARTZ_TASK_EXECUTOR = "meteringSyncQuartzTaskExecutor";

    private static final String METERING_SYNC_SCHEDULER = "meteringSyncScheduler";

    @Value("${metering.sync.quartz.threadpool.core.size:15}")
    private int meteringSyncQuartzCorePoolSize;

    @Value("${metering.sync.quartz.threadpool.max.size:15}")
    private int meteringSyncQuartzMaxPoolSize;

    @Value("${metering.sync.quartz.threadpool.capacity.size:-1}")
    private int meteringSyncQuartzQueueCapacity;

    @Value("${metering.sync.quartz.threadpool.thread.priority:5}")
    private int meteringSyncQuartzThreadPriority;

    @Value("${metering.sync.quartz.threadpool.custom.executor:true}")
    private boolean meteringSyncCustomExecutorEnabled;

    @Inject
    private MeterRegistry meterRegistry;

    @Bean
    public SchedulerFactoryBean meteringSyncScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
        meteringSyncSchedulerFactoryBeanCustomizer().customize(schedulerFactoryBean);
        return schedulerFactoryBean;
    }

    public SchedulerFactoryBeanCustomizer meteringSyncSchedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setSchedulerName(METERING_SYNC_SCHEDULER);
            if (meteringSyncCustomExecutorEnabled) {
                bean.setTaskExecutor(meteringSyncQuartzTaskExecutor());
            }
        };
    }

    @Bean(name = METERING_SYNC_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX)
    public ExecutorService meteringSyncQuartzTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(meteringSyncQuartzCorePoolSize);
        executor.setMaxPoolSize(meteringSyncQuartzMaxPoolSize);
        executor.setQueueCapacity(meteringSyncQuartzQueueCapacity == -1 ? Integer.MAX_VALUE : meteringSyncQuartzQueueCapacity);
        executor.setThreadPriority(meteringSyncQuartzThreadPriority);
        executor.setThreadNamePrefix(METERING_SYNC_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX);
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), METERING_SYNC_QUARTZ_TASK_EXECUTOR, "threadpool");
    }
}
