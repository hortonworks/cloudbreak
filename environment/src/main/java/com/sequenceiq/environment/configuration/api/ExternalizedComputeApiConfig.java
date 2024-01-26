package com.sequenceiq.environment.configuration.api;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.externalizedcompute.api.client.internal.ExternalizedComputeApiClientParams;

@Configuration
public class ExternalizedComputeApiConfig {
    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    @Named("externalizedComputeServerUrl")
    private String externalizedComputeServerUrl;

    @Bean
    public ExternalizedComputeApiClientParams externalizedComputeApiClientParams() {
        return new ExternalizedComputeApiClientParams(restDebug, certificateValidation, ignorePreValidation, externalizedComputeServerUrl);
    }
}
