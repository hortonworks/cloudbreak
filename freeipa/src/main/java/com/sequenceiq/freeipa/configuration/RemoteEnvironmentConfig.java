package com.sequenceiq.freeipa.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.remoteenvironment.api.client.internal.RemoteEnvironmentApiClientParams;

@Configuration
public class RemoteEnvironmentConfig {

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Inject
    @Named("remoteEnvironmentServiceUrl")
    private String remoteEnvironmentServiceUrl;

    @Bean
    public RemoteEnvironmentApiClientParams remoteEnvironmentApiClientParams() {
        return new RemoteEnvironmentApiClientParams(restDebug, certificateValidation, ignorePreValidation, remoteEnvironmentServiceUrl);
    }
}
