package com.sequenceiq.freeipa.api.client.internal;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.UserCrnClientRequestFilter;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

@Configuration
public class FreeIpaApiClientConfig {

    @Inject
    private UserCrnClientRequestFilter userCrnClientRequestFilter;

    @Inject
    private ClientTracingFeature clientTracingFeature;

    @Bean
    @ConditionalOnBean(FreeIpaApiClientParams.class)
    public WebTarget freeIpaApiClientWebTarget(FreeIpaApiClientParams freeIpaApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(freeIpaApiClientParams.getServiceUrl())
                .withCertificateValidation(freeIpaApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(freeIpaApiClientParams.isIgnorePreValidation())
                .withDebug(freeIpaApiClientParams.isRestDebug())
                .withClientRequestFilter(userCrnClientRequestFilter)
                .withTracer(clientTracingFeature)
                .withApiRoot(FreeIpaApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    LdapConfigV1Endpoint createLdapConfigV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, LdapConfigV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    KerberosConfigV1Endpoint createKerberosConfigV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, KerberosConfigV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    FreeIpaV1Endpoint freeIpaV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    KerberosMgmtV1Endpoint kerberosMgmtV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, KerberosMgmtV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    DnsV1Endpoint dnsV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, DnsV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    OperationV1Endpoint operationV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, OperationV1Endpoint.class);
    }
}
