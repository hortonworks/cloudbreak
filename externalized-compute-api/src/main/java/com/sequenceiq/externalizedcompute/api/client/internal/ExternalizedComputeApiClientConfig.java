package com.sequenceiq.externalizedcompute.api.client.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.externalizedcompute.api.ExternalizedComputeClusterApi;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterInternalEndpoint;

@Configuration
public class ExternalizedComputeApiClientConfig {

    @Inject
    private ApiClientRequestFilter apiClientRequestFilter;

    @Bean
    @ConditionalOnBean(ExternalizedComputeApiClientParams.class)
    public WebTarget externalizedComputeApiClientWebTarget(ExternalizedComputeApiClientParams externalizedComputeApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(externalizedComputeApiClientParams.getServiceUrl())
                .withCertificateValidation(externalizedComputeApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(externalizedComputeApiClientParams.isIgnorePreValidation())
                .withDebug(externalizedComputeApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(ExternalizedComputeClusterApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "externalizedComputeApiClientWebTarget")
    ExternalizedComputeClusterEndpoint createExternalizedComputeEndpoint(WebTarget externalizedComputeApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(externalizedComputeApiClientWebTarget, ExternalizedComputeClusterEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "externalizedComputeApiClientWebTarget")
    ExternalizedComputeClusterInternalEndpoint createExternalizedComputeInternalEndpoint(WebTarget externalizedComputeApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(externalizedComputeApiClientWebTarget, ExternalizedComputeClusterInternalEndpoint.class);
    }
}
