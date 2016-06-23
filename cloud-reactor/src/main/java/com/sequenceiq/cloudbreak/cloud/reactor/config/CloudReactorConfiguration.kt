package com.sequenceiq.cloudbreak.cloud.reactor.config

import java.util.concurrent.ScheduledThreadPoolExecutor

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder

@Configuration
class CloudReactorConfiguration {

    @Value("${cb.cloud.api.executorservice.pool.size:}")
    private val executorServicePoolSize: Int = 0

    @Bean
    internal fun listeningScheduledExecutorService(): ListeningScheduledExecutorService {
        return MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(executorServicePoolSize,
                ThreadFactoryBuilder().setNameFormat("cloud-reactor-%d").build()))
    }
}
