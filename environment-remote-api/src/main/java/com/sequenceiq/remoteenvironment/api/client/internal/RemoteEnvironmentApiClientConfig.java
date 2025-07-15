package com.sequenceiq.remoteenvironment.api.client.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.remoteenvironment.api.RemoteEnvironmentApi;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentV2Endpoint;

@Configuration
public class RemoteEnvironmentApiClientConfig {

    @Inject
    private ApiClientRequestFilter apiClientRequestFilter;

    @Bean
    @ConditionalOnBean(RemoteEnvironmentApiClientParams.class)
    public WebTarget remoteEnvironmentApiClientWebTarget(RemoteEnvironmentApiClientParams remoteEnvironmentApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(remoteEnvironmentApiClientParams.getServiceUrl())
                .withCertificateValidation(remoteEnvironmentApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(remoteEnvironmentApiClientParams.isIgnorePreValidation())
                .withDebug(remoteEnvironmentApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(RemoteEnvironmentApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "remoteEnvironmentApiClientWebTarget")
    RemoteEnvironmentEndpoint remoteEnvironmentEndpoint(WebTarget remoteEnvironmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(remoteEnvironmentApiClientWebTarget, RemoteEnvironmentEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "remoteEnvironmentV2ApiClientWebTarget")
    RemoteEnvironmentV2Endpoint remoteEnvironmentV2Endpoint(WebTarget remoteEnvironmentV2ApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(remoteEnvironmentV2ApiClientWebTarget, RemoteEnvironmentV2Endpoint.class);
    }
}
