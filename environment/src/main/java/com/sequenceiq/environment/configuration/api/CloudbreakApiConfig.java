package com.sequenceiq.environment.configuration.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.internal.CloudbreakApiClientParams;

@Configuration
public class CloudbreakApiConfig {

    @Inject
    @Named("cloudbreakServerUrl")
    private String cloudbreakServerUrl;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Bean
    public CloudbreakApiClientParams cloudbreakClient() {
        return new CloudbreakApiClientParams(restDebug, certificateValidation, ignorePreValidation, cloudbreakServerUrl);
    }
}
