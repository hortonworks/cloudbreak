package com.sequenceiq.environment.configuration;

import static com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService.DELAYED_TASK_EXECUTOR;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.environment.environment.dto.credential.CloudPlatformAwareCredentialDetailsConverter;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.environment.validation.securitygroup.EnvironmentSecurityGroupValidator;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.redbeams.client.internal.RedbeamsApiClientParams;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
@EnableRetry
public class AppConfig {

    private static final Logger LOGGER = getLogger(AppConfig.class);

    @Inject
    private List<EnvironmentNetworkValidator> environmentNetworkValidators;

    @Inject
    private List<EnvironmentSecurityGroupValidator> environmentSecurityGroupValidators;

    @Inject
    private List<EnvironmentNetworkConverter> environmentNetworkConverters;

    @Inject
    private List<EnvironmentParametersConverter> environmentParametersConverters;

    @Inject
    private List<CloudPlatformAwareCredentialDetailsConverter> credentialDetailsConverters;

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

    @Value("${cb.intermediate.threadpool.termination.seconds:60}")
    private int intermediateAwaitTerminationSeconds;

    @Value("${environment.delayed.threadpool.core.size:10}")
    private int delayedCorePoolSize;

    @Inject
    @Named("redbeamsServerUrl")
    private String redbeamsServerUrl;

    @Value("${environment.redbeams.supportedPlatforms:}")
    private String supportedRedbeamsPlatforms;

    @Value("${environment.freeipa.supportedPlatforms:}")
    private String supportedFreeIpaPlatforms;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Inject
    private MeterRegistry meterRegistry;

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
    public Map<CloudPlatform, CloudPlatformAwareCredentialDetailsConverter> cloudPlatformAwareCredentialDetailsConverterMap() {
        return credentialDetailsConverters
                .stream()
                .collect(Collectors.toMap(CloudPlatformAwareCredentialDetailsConverter::getCloudPlatform, Function.identity()));
    }

    @Bean
    @Primary
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        return commonExecutorServiceFactory.newAsyncTaskExecutor("intermediateBuilderExecutor", virtualThreadsAvailable, intermediateCorePoolSize,
                intermediateQueueCapacity, intermediateAwaitTerminationSeconds);
    }

    @Bean
    public RedbeamsApiClientParams redbeamsApiClientParams() {
        return new RedbeamsApiClientParams(restDebug, certificateValidation, ignorePreValidation, redbeamsServerUrl);
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public Client restClient() {
        return RestClientUtil.get(new ConfigKey(certificateValidation, restDebug, ignorePreValidation));
    }

    @Bean(name = DELAYED_TASK_EXECUTOR)
    public ScheduledExecutorService delayedTaskExecutor() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(delayedCorePoolSize);
        executor.setThreadNamePrefix("delayedExecutor-");
        executor.initialize();
        return ExecutorServiceMetrics.monitor(meterRegistry, executor.getScheduledExecutor(), DELAYED_TASK_EXECUTOR, "threadpool");
    }

    @Bean
    public SupportedPlatforms supportedFreeIpaPlatforms() {
        LOGGER.info("Supported freeipa platform: {}", supportedFreeIpaPlatforms);
        return new SupportedPlatforms(supportedFreeIpaPlatforms.split(","));
    }

}
