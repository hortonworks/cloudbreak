package com.sequenceiq.environment.client.internal;

import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.environment.api.EnvironmentApi;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.CredentialPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.tags.endpoint.AccountTagEndpoint;
import com.sequenceiq.environment.api.v1.telemetry.endpoint.AccountTelemetryEndpoint;

@Configuration
public class EnvironmentApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    public EnvironmentApiClientConfig(ApiClientRequestFilter apiClientRequestFilter) {
        this.apiClientRequestFilter = apiClientRequestFilter;
    }

    @Bean
    @ConditionalOnBean(EnvironmentApiClientParams.class)
    public WebTarget environmentApiClientWebTarget(EnvironmentApiClientParams environmentApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(environmentApiClientParams.getServiceUrl())
                .withCertificateValidation(environmentApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(environmentApiClientParams.isIgnorePreValidation())
                .withDebug(environmentApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(EnvironmentApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    CredentialEndpoint credentialEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, CredentialEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    ProxyEndpoint proxyEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, ProxyEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    EnvironmentEndpoint environmentApiEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, EnvironmentEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    CredentialPlatformResourceEndpoint credentialPlatformResourceEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, CredentialPlatformResourceEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, EnvironmentPlatformResourceEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    AccountTagEndpoint accountTagEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, AccountTagEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    AccountTelemetryEndpoint accountTelemetryEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, AccountTelemetryEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, AzureMarketplaceTermsEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "environmentApiClientWebTarget")
    EncryptionProfileEndpoint encryptionProfileEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, EncryptionProfileEndpoint.class);
    }

}
