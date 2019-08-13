package com.sequenceiq.datalake.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.datalake.logger.MDCContextFilter;
import com.sequenceiq.datalake.logger.ThreadBasedRequestIdProvider;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnClient;
import com.sequenceiq.redbeams.client.RedbeamsServiceClientBuilder;
import com.sequenceiq.redbeams.client.RedbeamsServiceCrnClient;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig implements AsyncConfigurer {

    @Inject
    @Named("cloudbreakUrl")
    private String cloudbreakUrl;

    @Inject
    @Named("environmentServerUrl")
    private String environmentServerUrl;

    @Inject
    @Named("redbeamsServerUrl")
    private String redbeamsServerUrl;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Value("${datalake.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${datalake.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    @Bean
    public CloudbreakServiceUserCrnClient cloudbreakClient() {
        return new CloudbreakUserCrnClientBuilder(cloudbreakUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public EnvironmentServiceCrnClient environmentServiceClient() {
        return new EnvironmentServiceClientBuilder(environmentServerUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public RedbeamsServiceCrnClient redbeamsServiceCrnClient() {
        return new RedbeamsServiceClientBuilder(redbeamsServerUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public FilterRegistrationBean<MDCContextFilter> mdcContextFilterRegistrationBean() {
        FilterRegistrationBean<MDCContextFilter> registrationBean = new FilterRegistrationBean<>();
        MDCContextFilter filter = new MDCContextFilter(threadBasedUserCrnProvider, threadBasedRequestIdProvider, null);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
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
