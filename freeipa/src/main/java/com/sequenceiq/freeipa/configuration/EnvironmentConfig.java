package com.sequenceiq.freeipa.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.environment.client.internal.EnvironmentApiClientParams;

@Configuration
public class EnvironmentConfig {

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Inject
    @Named("environmentServiceUrl")
    private String environmentServiceUrl;

    @Bean
    public EnvironmentApiClientParams environmentApiClientParams() {
        return new EnvironmentApiClientParams(restDebug, certificateValidation, ignorePreValidation, environmentServiceUrl);
    }
}
