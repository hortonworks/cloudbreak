package com.sequenceiq.remoteenvironment.api.client.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;

@Configuration
public class RemoteEnvironmentApiClientConfig {

    @Inject
    private ApiClientRequestFilter apiClientRequestFilter;

    @Bean
    @ConditionalOnBean(name = "remoteEnvironmentApiClientWebTarget")
    RemoteEnvironmentEndpoint remoteEnvironmentEndpoint(WebTarget remoteEnvironmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(remoteEnvironmentApiClientWebTarget, RemoteEnvironmentEndpoint.class);
    }
}
