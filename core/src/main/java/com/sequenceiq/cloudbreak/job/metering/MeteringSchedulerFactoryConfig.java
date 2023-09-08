package com.sequenceiq.cloudbreak.job.metering;

import static com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryConfig.METERING_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.sql.DataSource;

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
public class MeteringSchedulerFactoryConfig {

    private static final String METERING_QUARTZ_TASK_EXECUTOR = "meteringQuartzTaskExecutor";

    private static final String METERING_SCHEDULER = "meteringScheduler";

    @Value("${metering.quartz.threadpool.core.size:5}")
    private int meteringQuartzCorePoolSize;

    @Value("${metering.quartz.threadpool.max.size:5}")
    private int meteringQuartzMaxPoolSize;

    @Value("${metering.quartz.threadpool.capacity.size:-1}")
    private int meteringQuartzQueueCapacity;

    @Value("${metering.quartz.threadpool.thread.priority:5}")
    private int meteringQuartzThreadPriority;

    @Value("${metering.quartz.threadpool.custom.executor:true}")
    private boolean meteringCustomExecutorEnabled;

    @Inject
    private MeterRegistry meterRegistry;

    @Bean
    public SchedulerFactoryBean meteringScheduler(QuartzProperties quartzProperties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext, DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = SchedulerFactoryBeanUtil.createSchedulerFactoryBean(quartzProperties, customizers, applicationContext);
        meteringSchedulerFactoryBeanCustomizer().customize(schedulerFactoryBean);
        return schedulerFactoryBean;
    }

    public SchedulerFactoryBeanCustomizer meteringSchedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setSchedulerName(METERING_SCHEDULER);
            if (meteringCustomExecutorEnabled) {
                bean.setTaskExecutor(meteringQuartzTaskExecutor());
            }
        };
    }

    @Bean(name = METERING_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX)
    public ExecutorService meteringQuartzTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(meteringQuartzCorePoolSize);
        executor.setMaxPoolSize(meteringQuartzMaxPoolSize);
        executor.setQueueCapacity(meteringQuartzQueueCapacity == -1 ? Integer.MAX_VALUE : meteringQuartzQueueCapacity);
        executor.setThreadPriority(meteringQuartzThreadPriority);
        executor.setThreadNamePrefix(METERING_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX);
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), METERING_QUARTZ_TASK_EXECUTOR, "threadpool");
    }
}
