package com.sequenceiq.datalake.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.datalake.logger.MDCContextFilter;
import com.sequenceiq.environment.client.EnvironmentServiceClient;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;

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

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Bean
    public CloudbreakUserCrnClient cloudbreakClient() {
        return new CloudbreakUserCrnClientBuilder(cloudbreakUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public EnvironmentServiceClient environmentServiceClient() {
        return new EnvironmentServiceClientBuilder(environmentServerUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }

    @Bean
    public FilterRegistrationBean<MDCContextFilter> mdcContextFilterRegistrationBean() {
        FilterRegistrationBean<MDCContextFilter> registrationBean = new FilterRegistrationBean<>();
        MDCContextFilter filter = new MDCContextFilter(threadBasedUserCrnProvider, null);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }

}
