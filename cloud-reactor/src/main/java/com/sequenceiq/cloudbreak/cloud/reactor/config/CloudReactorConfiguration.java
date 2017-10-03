package com.sequenceiq.cloudbreak.cloud.reactor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerScheduledExecutor;

@Configuration
public class CloudReactorConfiguration {

    @Value("${cb.cloud.api.executorservice.pool.size:}")
    private int executorServicePoolSize;

    @Bean
    ListeningScheduledExecutorService listeningScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServicePoolSize,
                        new ThreadFactoryBuilder().setNameFormat("cloud-reactor-%d").build()));
    }
}
