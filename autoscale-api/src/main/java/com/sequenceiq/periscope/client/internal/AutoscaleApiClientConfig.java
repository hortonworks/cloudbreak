package com.sequenceiq.periscope.client.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleYarnRecommendationV1Endpoint;

@Configuration
public class AutoscaleApiClientConfig {

    @Inject
    private ApiClientRequestFilter apiClientRequestFilter;

    @Bean
    @ConditionalOnBean(AutoscaleApiClientParams.class)
    public WebTarget autoscaleApiClientWebTarget(AutoscaleApiClientParams autoscaleApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(autoscaleApiClientParams.getServiceUrl())
                .withCertificateValidation(autoscaleApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(autoscaleApiClientParams.isIgnorePreValidation())
                .withDebug(autoscaleApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withClientConnectionTimeout(autoscaleApiClientParams.getConnectionTimeout())
                .withClientReadTimeout(autoscaleApiClientParams.getReadTimeout())
                .withApiRoot(AutoscaleApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "autoscaleApiClientWebTarget")
    DistroXAutoScaleYarnRecommendationV1Endpoint createAutoScaleYarnRecommendationV1Endpoint(WebTarget autoscaleApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(autoscaleApiClientWebTarget, DistroXAutoScaleYarnRecommendationV1Endpoint.class);
    }
}
