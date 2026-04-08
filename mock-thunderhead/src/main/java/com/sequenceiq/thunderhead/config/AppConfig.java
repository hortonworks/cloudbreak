package com.sequenceiq.thunderhead.config;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;

@Configuration
public class AppConfig {

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Value("${mockthunderhead.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${mockthunderhead.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Value("${mockthunderhead.intermediate.threadpool.termination.seconds:60}")
    private int intermediateAwaitTerminationSeconds;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Bean
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        return commonExecutorServiceFactory.newAsyncTaskExecutor("intermediateBuilderExecutor", virtualThreadsAvailable, intermediateCorePoolSize,
                intermediateQueueCapacity, intermediateAwaitTerminationSeconds);
    }
}
