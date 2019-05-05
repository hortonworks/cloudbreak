package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;

@Configuration
public class CloudbreakExecutorServiceConfiguration {

    @Value("${cb.executorservice.pool.size:40}")
    private int executorServicePoolSize;

    @Bean
    ListeningScheduledExecutorService cloudbreakListeningScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServicePoolSize,
                        new ThreadFactoryBuilder().setNameFormat("cloud-api-%d").build()));
    }
}
