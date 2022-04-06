package com.sequenceiq.periscope.service.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.WebTarget;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.freeipa.api.client.internal.FreeIpaApiClientParams;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;

@Configuration
public class FreeIpaClientConfiguration {

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    @Named("freeIpaServerUrl")
    private String freeIpaServerUrl;

    @Bean
    public FreeIpaApiClientParams freeIpaApiClientParams() {
        return new FreeIpaApiClientParams(restDebug, certificateValidation, ignorePreValidation, freeIpaServerUrl);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    UserV1Endpoint userV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, UserV1Endpoint.class);
    }
}

