package com.sequenceiq.environment.configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.converter.network.EnvironmentNetworkConverter;
import com.sequenceiq.environment.environment.validator.network.EnvironmentNetworkValidator;

@Configuration
public class AppConfig {

    @Inject
    private List<EnvironmentNetworkValidator> environmentNetworkValidators;

    @Inject
    private List<EnvironmentNetworkConverter> environmentNetworkConverters;

    @Value("${cb.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${cb.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Bean
    public Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform() {
        return environmentNetworkValidators
                .stream()
                .collect(Collectors.toMap(EnvironmentNetworkValidator::getCloudPlatform, x -> x));
    }

    @Bean
    public Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConvertersByCloudPlatform() {
        return environmentNetworkConverters
                .stream()
                .collect(Collectors.toMap(EnvironmentNetworkConverter::getCloudPlatform, x -> x));
    }

    @Bean
    @Primary
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(intermediateCorePoolSize);
        executor.setQueueCapacity(intermediateQueueCapacity);
        executor.setThreadNamePrefix("intermediateBuilderExecutor-");
        executor.setTaskDecorator(new MDCCleanerTaskDecorator());
        executor.initialize();
        return executor;
    }
}
