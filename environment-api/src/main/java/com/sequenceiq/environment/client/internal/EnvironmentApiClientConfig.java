package com.sequenceiq.environment.client.internal;

import jakarta.ws.rs.client.WebTarget;

import org.springframework.context.annotation.Bean;

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

/**
 * Environment API client beans. Intentionally not a {@code @Configuration}, so component scanning does not pick it up:
 * import it with {@code @Import} from the configuration that defines the {@link EnvironmentApiClientParams} bean.
 * This keeps bean creation independent of configuration class processing order.
 */
public class EnvironmentApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    public EnvironmentApiClientConfig(ApiClientRequestFilter apiClientRequestFilter) {
        this.apiClientRequestFilter = apiClientRequestFilter;
    }

    @Bean
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
    CredentialEndpoint credentialEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, CredentialEndpoint.class);
    }

    @Bean
    ProxyEndpoint proxyEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, ProxyEndpoint.class);
    }

    @Bean
    EnvironmentEndpoint environmentApiEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, EnvironmentEndpoint.class);
    }

    @Bean
    CredentialPlatformResourceEndpoint credentialPlatformResourceEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, CredentialPlatformResourceEndpoint.class);
    }

    @Bean
    EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, EnvironmentPlatformResourceEndpoint.class);
    }

    @Bean
    AccountTagEndpoint accountTagEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, AccountTagEndpoint.class);
    }

    @Bean
    AccountTelemetryEndpoint accountTelemetryEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, AccountTelemetryEndpoint.class);
    }

    @Bean
    AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, AzureMarketplaceTermsEndpoint.class);
    }

    @Bean
    EncryptionProfileEndpoint encryptionProfileEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, EncryptionProfileEndpoint.class);
    }

}
