package com.sequenceiq.cloudbreak.conf;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClientBuilder;

@Configuration
public class EnvironmentInternalClientConfiguration {

    @Inject
    @Named("environmentServerUrl")
    private String environmentUrl;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public EnvironmentServiceUserCrnClient environmentClient() {
        return new EnvironmentServiceUserCrnClientBuilder(environmentUrl)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
    }

    @Bean
    public EnvironmentInternalCrnClient cloudbreakInternalCrnClientClient() {
        return new EnvironmentInternalCrnClient(environmentClient(), internalCrnBuilder());
    }

    public RegionAwareInternalCrnGenerator internalCrnBuilder() {
        return regionAwareInternalCrnGeneratorFactory.coreAdmin();
    }
}
