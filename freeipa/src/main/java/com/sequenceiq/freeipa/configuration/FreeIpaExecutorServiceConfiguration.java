package com.sequenceiq.freeipa.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;

@Configuration
public class FreeIpaExecutorServiceConfiguration {

    @Value("${freeipa.executorservice.pool.size:40}")
    private int executorServicePoolSize;

    @Bean
    ListeningScheduledExecutorService freeipaListeningScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServicePoolSize,
                        new ThreadFactoryBuilder().setNameFormat("freeipa-%d").build()));
    }
}
