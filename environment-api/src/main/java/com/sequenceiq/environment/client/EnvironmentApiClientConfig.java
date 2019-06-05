package com.sequenceiq.environment.client;

import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.UserCrnClientRequestFilter;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.environment.api.EnvironmentApi;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;

@Configuration
public class EnvironmentApiClientConfig {

    private final UserCrnClientRequestFilter userCrnClientRequestFilter;

    public EnvironmentApiClientConfig(UserCrnClientRequestFilter userCrnClientRequestFilter) {
        this.userCrnClientRequestFilter = userCrnClientRequestFilter;
    }

    @Bean
    @ConditionalOnBean(EnvironmentApiClientParams.class)
    public WebTarget environmentApiClientWebTarget(EnvironmentApiClientParams environmentApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(environmentApiClientParams.getServiceUrl())
                .withCertificateValidation(environmentApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(environmentApiClientParams.isIgnorePreValidation())
                .withDebug(environmentApiClientParams.isRestDebug())
                .withClientRequestFilter(userCrnClientRequestFilter)
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
    PlatformResourceEndpoint platformResourceEndpoint(WebTarget environmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(environmentApiClientWebTarget, PlatformResourceEndpoint.class);
    }
}
