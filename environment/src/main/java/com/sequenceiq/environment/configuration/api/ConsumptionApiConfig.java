package com.sequenceiq.environment.configuration.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.consumption.client.internal.ConsumptionApiClientParams;

@Configuration
public class ConsumptionApiConfig {

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    @Named("consumptionServerUrl")
    private String consumptionServerUrl;

    @Bean
    public ConsumptionApiClientParams consumptionApiClientParams() {
        return new ConsumptionApiClientParams(restDebug, certificateValidation, ignorePreValidation, consumptionServerUrl);
    }

}
