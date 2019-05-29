package com.sequenceiq.freeipa.api.client;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.UserCrnClientRequestFilter;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;

@Configuration
public class FreeIpaApiClientConfig {
    @Inject
    private UserCrnClientRequestFilter userCrnClientRequestFilter;

    @Bean
    @ConditionalOnBean(FreeIpaApiClientParams.class)
    public WebTarget freeIpaApiClientWebTarget(FreeIpaApiClientParams freeIpaApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(freeIpaApiClientParams.getServiceUrl())
                .withCertificateValidation(freeIpaApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(freeIpaApiClientParams.isIgnorePreValidation())
                .withDebug(freeIpaApiClientParams.isRestDebug())
                .withClientRequestFilter(userCrnClientRequestFilter)
                .withApiRoot(FreeIpaApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    LdapConfigV1Endpoint createLdapConfigV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, LdapConfigV1Endpoint.class);
    }
}
