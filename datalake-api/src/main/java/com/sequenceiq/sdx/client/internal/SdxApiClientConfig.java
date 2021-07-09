package com.sequenceiq.sdx.client.internal;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.sdx.api.SdxApi;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.ProgressEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

@Configuration
public class SdxApiClientConfig {

    @Inject
    private ApiClientRequestFilter apiClientRequestFilter;

    @Inject
    private ClientTracingFeature clientTracingFeature;

    @Bean
    @ConditionalOnBean(SdxApiClientParams.class)
    public WebTarget sdxApiClientWebTarget(SdxApiClientParams sdxApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(sdxApiClientParams.getServiceUrl())
                .withCertificateValidation(sdxApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(sdxApiClientParams.isIgnorePreValidation())
                .withDebug(sdxApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(SdxApi.API_ROOT_CONTEXT)
                .withTracer(clientTracingFeature)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxEndpoint createSdxV1Endpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    ProgressEndpoint createSdxV1ProgressEndpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, ProgressEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    OperationEndpoint createSdxV1OperationEndpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, OperationEndpoint.class);
    }
}
