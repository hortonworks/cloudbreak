package com.sequenceiq.periscope.config;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;
import com.sequenceiq.periscope.utils.MetricUtils;

@Configuration
public class PeriscopeExecutorServiceConfiguration {

    @Value("${periscope.executorservice.pool.size:40}")
    private int executorServicePoolSize;

    @Inject
    private MetricUtils metricUtils;

    @Bean
    ListeningScheduledExecutorService periscopeListeningScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServicePoolSize,
                        new ThreadFactoryBuilder().setNameFormat("autoscale-%d").build(), metricUtils::submitThreadPoolExecutorParameters));
    }
}
