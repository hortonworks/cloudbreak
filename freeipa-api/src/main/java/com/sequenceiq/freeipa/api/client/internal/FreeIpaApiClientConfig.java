package com.sequenceiq.freeipa.api.client.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.co2.FreeIpaCO2V1Endpoint;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.crossrealm.TrustV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.EncryptionV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaInternalV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.FreeIpaUpgradeV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.progress.ProgressV1Endpoint;
import com.sequenceiq.freeipa.api.v1.recipe.RecipeV1Endpoint;
import com.sequenceiq.freeipa.api.v1.support.SupportV1Endpoint;
import com.sequenceiq.freeipa.api.v1.util.UtilV1Endpoint;
import com.sequenceiq.freeipa.api.v2.freeipa.FreeIpaV2Endpoint;
import com.sequenceiq.freeipa.api.v2.freeipa.crossrealm.TrustV2Endpoint;

@Configuration
public class FreeIpaApiClientConfig {

    @Inject
    private ApiClientRequestFilter apiClientRequestFilter;

    @Bean
    @ConditionalOnBean(FreeIpaApiClientParams.class)
    public WebTarget freeIpaApiClientWebTarget(FreeIpaApiClientParams freeIpaApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(freeIpaApiClientParams.getServiceUrl())
                .withCertificateValidation(freeIpaApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(freeIpaApiClientParams.isIgnorePreValidation())
                .withDebug(freeIpaApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
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
    FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaInternalV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    TrustV1Endpoint trustV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, TrustV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    TrustV2Endpoint trustV2Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, TrustV2Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    SupportV1Endpoint freeIpaSupportV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, SupportV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    FreeIpaV2Endpoint freeIpaV2Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaV2Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaRotationV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    FreeIpaCostV1Endpoint freeIpaCostV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaCostV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    FreeIpaCO2V1Endpoint freeIpaCO2V1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaCO2V1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    UserV1Endpoint userV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, UserV1Endpoint.class);
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

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    ProgressV1Endpoint progressV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, ProgressV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    FreeIpaUpgradeV1Endpoint freeIpaUpgradeV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaUpgradeV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    UtilV1Endpoint utilV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, UtilV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    RecipeV1Endpoint recipeV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, RecipeV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    FreeIpaV1FlowEndpoint freeIpaV1FlowEndpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, FreeIpaV1FlowEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "freeIpaApiClientWebTarget")
    EncryptionV1Endpoint encryptionV1Endpoint(WebTarget freeIpaApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(freeIpaApiClientWebTarget, EncryptionV1Endpoint.class);
    }
}
