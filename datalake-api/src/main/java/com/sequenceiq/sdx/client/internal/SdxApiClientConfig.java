package com.sequenceiq.sdx.client.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.sdx.api.SdxApi;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.ProgressEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxBackupEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxFlowEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxRestoreEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxRotationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.endpoint.SupportV1Endpoint;

@Configuration
public class SdxApiClientConfig {

    @Inject
    private ApiClientRequestFilter apiClientRequestFilter;

    @Bean
    @ConditionalOnBean(SdxApiClientParams.class)
    public WebTarget sdxApiClientWebTarget(SdxApiClientParams sdxApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(sdxApiClientParams.getServiceUrl())
                .withCertificateValidation(sdxApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(sdxApiClientParams.isIgnorePreValidation())
                .withDebug(sdxApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(SdxApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxEndpoint createSdxV1Endpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxRotationEndpoint createSdxRotationEndpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxRotationEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxInternalEndpoint createSdxInternalEndpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxInternalEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxUpgradeEndpoint createSdxUpgradeV1Endpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxUpgradeEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxRestoreEndpoint createSdxRestoreV1Endpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxRestoreEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxBackupEndpoint createSdxBackupV1Endpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxBackupEndpoint.class);
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

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SdxFlowEndpoint createSdxV1FlowEndpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SdxFlowEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "sdxApiClientWebTarget")
    SupportV1Endpoint createSupportV1Endpoint(WebTarget sdxApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(sdxApiClientWebTarget, SupportV1Endpoint.class);
    }
}
