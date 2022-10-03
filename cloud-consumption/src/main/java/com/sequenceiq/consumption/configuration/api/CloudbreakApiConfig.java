package com.sequenceiq.consumption.configuration.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;

@Configuration
public class CloudbreakApiConfig {

    @Inject
    @Named("cloudbreakServerUrl")
    private String cloudbreakServerUrl;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private CloudbreakServiceUserCrnClient cloudbreakClient() {
        return new CloudbreakUserCrnClientBuilder(cloudbreakServerUrl)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
    }

    @Bean
    public CloudbreakInternalCrnClient client() {
        return new CloudbreakInternalCrnClient(cloudbreakClient(), internalCrnBuilder());
    }

    private RegionAwareInternalCrnGenerator internalCrnBuilder() {
        return regionAwareInternalCrnGeneratorFactory.coreAdmin();
    }
}
