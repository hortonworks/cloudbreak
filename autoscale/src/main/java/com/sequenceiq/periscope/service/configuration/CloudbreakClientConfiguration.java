package com.sequenceiq.periscope.service.configuration;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;

@Configuration
public class CloudbreakClientConfiguration {

    @Autowired
    @Qualifier("cloudbreakUrl")
    private String cloudbreakUrl;

    @Value("${cb.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Bean
    public CloudbreakServiceUserCrnClient cloudbreakClient() {
        CloudbreakServiceUserCrnClient client = new CloudbreakUserCrnClientBuilder(cloudbreakUrl + cbRootContextPath)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return client;
    }

    @Bean
    public CloudbreakInternalCrnClient cloudbreakInternalCrnClientClient() {
        return new CloudbreakInternalCrnClient(cloudbreakClient(), internalCrnBuilder());
    }

    @Bean
    public RegionAwareInternalCrnGenerator internalCrnBuilder() {
        return regionAwareInternalCrnGeneratorFactory.autoscale();
    }
}
