package com.sequenceiq.sdx.client;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.UserCrnClientRequestFilter;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.sdx.api.SdxApi;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;

@Configuration
public class SdxApiClientConfig {
    @Inject
    private UserCrnClientRequestFilter userCrnClientRequestFilter;

    @Bean
    @ConditionalOnBean(SdxApiClientParams.class)
    public WebTarget sdxApiClientWebTarget(SdxApiClientParams sdxApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(sdxApiClientParams.getServiceUrl())
                .withCertificateValidation(sdxApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(sdxApiClientParams.isIgnorePreValidation())
                .withDebug(sdxApiClientParams.isRestDebug())
                .withClientRequestFilter(userCrnClientRequestFilter)
                .withApiRoot(SdxApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxEndpoint createSdxV1Endpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxEndpoint.class);
    }
}
