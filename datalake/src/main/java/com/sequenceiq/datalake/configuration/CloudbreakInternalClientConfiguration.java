package com.sequenceiq.datalake.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;

@Configuration
public class CloudbreakInternalClientConfiguration {

    @Inject
    @Named("cloudbreakUrl")
    private String cloudbreakUrl;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public CloudbreakServiceUserCrnClient cloudbreakClient() {
        return new CloudbreakUserCrnClientBuilder(cloudbreakUrl)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
    }

    @Bean
    public CloudbreakInternalCrnClient cloudbreakInternalCrnClientClient() {
        return new CloudbreakInternalCrnClient(cloudbreakClient(), internalCrnBuilder());
    }

    public RegionAwareInternalCrnGenerator internalCrnBuilder() {
        return regionAwareInternalCrnGeneratorFactory.sdxAdmin();
    }
}
