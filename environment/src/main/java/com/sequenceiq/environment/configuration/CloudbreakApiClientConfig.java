package com.sequenceiq.environment.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;

@Configuration
public class CloudbreakApiClientConfig {

    @Inject
    @Named("cloudbreakUrl")
    private String cloudbreakUrl;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Bean
    public CloudbreakUserCrnClient cloudbreakClient() {
        return new CloudbreakUserCrnClientBuilder(cloudbreakUrl)
                .withCertificateValidation(certificateValidation)
                .withIgnorePreValidation(ignorePreValidation)
                .withDebug(restDebug)
                .build();
    }
}
