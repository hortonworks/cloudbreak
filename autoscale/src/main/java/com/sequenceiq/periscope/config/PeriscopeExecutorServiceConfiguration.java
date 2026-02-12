package com.sequenceiq.periscope.config;

import jakarta.inject.Inject;

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

    @Value("${periscope.executorservice.pool.size:60}")
    private int executorServicePoolSize;

    @Value("${periscope.executorservice.monitor.time.pool.size:20}")
    private int executorServiceTimeMonitorPoolSize;

    @Value("${periscope.executorservice.delete.pool.size:10}")
    private int executorServiceDeletePoolSize;

    @Inject
    private MetricUtils metricUtils;

    @Bean
    ListeningScheduledExecutorService periscopeListeningScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServicePoolSize,
                        new ThreadFactoryBuilder().setNameFormat("autoscale-%d").build(), metricUtils::submitThreadPoolExecutorParameters));
    }

    @Bean
    ListeningScheduledExecutorService periscopeDeleteScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServiceDeletePoolSize,
                        new ThreadFactoryBuilder().setNameFormat("autoscale-delete-monitor-%d").build()));
    }

    @Bean
    ListeningScheduledExecutorService periscopeTimeMonitorScheduledExecutorService() {
        return MoreExecutors
                .listeningDecorator(new MDCCleanerScheduledExecutor(executorServiceTimeMonitorPoolSize,
                        new ThreadFactoryBuilder().setNameFormat("autoscale-time-monitor-%d").build(), metricUtils::submitThreadPoolExecutorParameters));
    }
}
