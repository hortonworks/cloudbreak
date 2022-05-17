package com.sequenceiq.environment.configuration.api;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.consumption.client.ConsumptionInternalCrnClient;
import com.sequenceiq.consumption.client.ConsumptionServiceUserCrnClient;
import com.sequenceiq.consumption.client.ConsumptionServiceUserCrnClientBuilder;

@Configuration
public class ConsumptionInternalClientConfiguration {

    @Value("${environment.consumption.url:}")
    private String consumptionUrl;

    @Value("${environment.consumption.contextPath:}")
    private String consumptionContextPath;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public ConsumptionServiceUserCrnClient consumptionClient() {
        return new ConsumptionServiceUserCrnClientBuilder(consumptionUrl + consumptionContextPath)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
    }

    @Bean
    public ConsumptionInternalCrnClient consumptionInternalCrnClientClient() {
        return new ConsumptionInternalCrnClient(consumptionClient(), regionAwareInternalCrnGeneratorFactory);
    }
}
