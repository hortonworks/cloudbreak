package com.sequenceiq.remoteenvironment.config;

import static com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService.DELAYED_TASK_EXECUTOR;

import java.util.concurrent.ScheduledExecutorService;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
public class AppConfig implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Value("${remoteenvironment.delayed.threadpool.core.size:10}")
    private int delayedCorePoolSize;

    @Inject
    private MeterRegistry meterRegistry;

    @Bean(name = DELAYED_TASK_EXECUTOR)
    public ScheduledExecutorService delayedTaskExecutor() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(delayedCorePoolSize);
        executor.setThreadNamePrefix("delayedExecutor-");
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getScheduledExecutor(), DELAYED_TASK_EXECUTOR, "threadpool");
    }
}
