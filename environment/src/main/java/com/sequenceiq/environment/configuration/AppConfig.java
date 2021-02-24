package com.sequenceiq.environment.configuration;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.environment.validation.securitygroup.EnvironmentSecurityGroupValidator;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.redbeams.client.internal.RedbeamsApiClientParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@EnableRetry
public class AppConfig {

    @Inject
    private List<EnvironmentNetworkValidator> environmentNetworkValidators;

    @Inject
    private List<EnvironmentSecurityGroupValidator> environmentSecurityGroupValidators;

    @Inject
    private List<EnvironmentNetworkConverter> environmentNetworkConverters;

    @Inject
    private List<EnvironmentParametersConverter> environmentParametersConverters;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Value("${cb.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${cb.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Inject
    @Named("redbeamsServerUrl")
    private String redbeamsServerUrl;

    @Value("${environment.redbeams.supportedPlatforms:}")
    private String supportedRedbeamsPlatforms;

    @Value("${environment.freeipa.supportedPlatforms:}")
    private String supportedFreeIpaPlatforms;

    @Bean
    public Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform() {
        return environmentNetworkValidators
                .stream()
                .collect(Collectors.toMap(EnvironmentNetworkValidator::getCloudPlatform, Function.identity()));
    }

    @Bean
    public Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConvertersByCloudPlatform() {
        return environmentNetworkConverters
                .stream()
                .collect(Collectors.toMap(EnvironmentNetworkConverter::getCloudPlatform, Function.identity()));
    }

    @Bean
    public Map<CloudPlatform, EnvironmentSecurityGroupValidator> environmentSecurityGroupValidatorsByCloudPlatform() {
        return environmentSecurityGroupValidators
                .stream()
                .collect(Collectors.toMap(EnvironmentSecurityGroupValidator::getCloudPlatform, Function.identity()));
    }

    @Bean
    public Map<CloudPlatform, EnvironmentParametersConverter> environmentParametersConvertersByCloudPlatform() {
        return environmentParametersConverters
                .stream()
                .collect(Collectors.toMap(EnvironmentParametersConverter::getCloudPlatform, Function.identity()));
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

    @Bean
    public RedbeamsApiClientParams redbeamsApiClientParams() {
        return new RedbeamsApiClientParams(restDebug, certificateValidation, ignorePreValidation, redbeamsServerUrl);
    }

    @Bean
    public Client restClient() {
        return RestClientUtil.get(new ConfigKey(certificateValidation, restDebug, ignorePreValidation));
    }

    @Bean
    public SupportedPlatforms supportedFreeIpaPlatforms() {
        return new SupportedPlatforms(supportedFreeIpaPlatforms.split(","));
    }

}
