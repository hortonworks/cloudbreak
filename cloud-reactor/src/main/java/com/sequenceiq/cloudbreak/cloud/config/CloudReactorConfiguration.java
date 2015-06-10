package com.sequenceiq.cloudbreak.cloud.config;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Configuration
public class CloudReactorConfiguration {

    @Value("${cb.cloud.api.executorservice.pool.size:10}")
    private int executorServicePoolSize;

    @Bean
    ListeningScheduledExecutorService listeningScheduledExecutorService() {
        return MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(executorServicePoolSize));
    }
}
