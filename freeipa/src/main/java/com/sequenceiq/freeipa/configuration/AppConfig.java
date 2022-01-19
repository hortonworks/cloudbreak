package com.sequenceiq.freeipa.configuration;

import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.concurrent.TracingAndMdcCopyingTaskDecorator;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteria;
import com.sequenceiq.freeipa.service.filter.NetworkFilterProvider;

import io.opentracing.Tracer;

@Configuration
@EnableRetry
@EnableScheduling
public class AppConfig {

    @Value("${freeipa.intermediate.threadpool.core.size}")
    private int intermediateCorePoolSize;

    @Value("${freeipa.intermediate.threadpool.capacity.size}")
    private int intermediateQueueCapacity;

    @Inject
    private List<NetworkFilterProvider> networkFilterProviders;

    @Inject
    private Tracer tracer;

    @PostConstruct
    public void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    @Primary
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(intermediateCorePoolSize);
        executor.setQueueCapacity(intermediateQueueCapacity);
        executor.setThreadNamePrefix("intermediateBuilderExecutor-");
        executor.setTaskDecorator(new TracingAndMdcCopyingTaskDecorator(tracer));
        executor.initialize();
        return executor;
    }

    @Bean
    public Map<CloudPlatform, NetworkFilterProvider> networkFilterProviderMap() {
        Map<CloudPlatform, NetworkFilterProvider> result = new HashMap<>();
        for (NetworkFilterProvider networkFilterProvider : networkFilterProviders) {
            result.put(networkFilterProvider.cloudPlatform(), networkFilterProvider);
        }
        return result;
    }

    @Bean
    public ExitCriteria stackBasedExitCriteria() {
        return new StackBasedExitCriteria();
    }
}