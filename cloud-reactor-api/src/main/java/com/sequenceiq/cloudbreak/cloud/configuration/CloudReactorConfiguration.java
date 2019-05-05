package com.sequenceiq.cloudbreak.cloud.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;

@Configuration
public class CloudReactorConfiguration {

    @Value("${cb.reactor.executorservice.pool.size:40}")
    private int executorServicePoolSize;

    @Bean
    ListeningScheduledExecutorService reactorListeningScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServicePoolSize,
                        new ThreadFactoryBuilder().setNameFormat("cloud-reactor-%d").build()));
    }
}
