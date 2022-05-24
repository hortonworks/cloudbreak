package com.sequenceiq.environment.configuration.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.consumption.client.ConsumptionInternalCrnClient;
import com.sequenceiq.consumption.client.ConsumptionServiceUserCrnClient;
import com.sequenceiq.consumption.client.ConsumptionServiceUserCrnClientBuilder;

@Configuration
public class ConsumptionInternalClientConfiguration {

    @Inject
    @Named("consumptionServerUrl")
    private String consumptionServerUrl;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private ConsumptionServiceUserCrnClient consumptionClient() {
        return new ConsumptionServiceUserCrnClientBuilder(consumptionServerUrl)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
    }

    private RegionAwareInternalCrnGenerator internalCrnBuilder() {
        return regionAwareInternalCrnGeneratorFactory.iam();
    }

    @Bean
    public ConsumptionInternalCrnClient consumptionInternalCrnClient() {
        return new ConsumptionInternalCrnClient(consumptionClient(), internalCrnBuilder());
    }

}
