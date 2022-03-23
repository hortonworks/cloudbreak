package com.sequenceiq.periscope.service.configuration;

import javax.inject.Inject;

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

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

@Configuration
public class CloudbreakClientConfiguration {

    @Autowired
    @Qualifier("cloudbreakUrl")
    private String cloudbreakUrl;

    @Value("${cb.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Inject
    private ClientTracingFeature clientTracingFeature;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Bean
    public CloudbreakServiceUserCrnClient cloudbreakClient() {
        CloudbreakServiceUserCrnClient client = new CloudbreakUserCrnClientBuilder(cloudbreakUrl + cbRootContextPath)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        client.registerClientTracingFeature(clientTracingFeature);
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
