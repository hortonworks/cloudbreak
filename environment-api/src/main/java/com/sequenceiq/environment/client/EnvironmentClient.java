package com.sequenceiq.environment.client;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.AuditCredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.CrossRealmTrustEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentDefaultComputeClusterEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentHybridEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentInternalEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.terms.endpoint.TermsEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public interface EnvironmentClient {
    <E> E getEndpoint(Class<E> clazz);

    default CredentialEndpoint credentialV1Endpoint() {
        return getEndpoint(CredentialEndpoint.class);
    }

    default AuditCredentialEndpoint auditCredentialV1Endpoint() {
        return getEndpoint(AuditCredentialEndpoint.class);
    }

    default ProxyEndpoint proxyV1Endpoint() {
        return getEndpoint(ProxyEndpoint.class);
    }

    default EnvironmentEndpoint environmentV1Endpoint() {
        return getEndpoint(EnvironmentEndpoint.class);
    }

    default EnvironmentInternalEndpoint environmentInternalEndpoint() {
        return getEndpoint(EnvironmentInternalEndpoint.class);
    }

    default EnvironmentDefaultComputeClusterEndpoint defaultComputeClusterEndpoint() {
        return getEndpoint(EnvironmentDefaultComputeClusterEndpoint.class);
    }

    default FlowEndpoint flowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }

    default FlowPublicEndpoint flowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }

    default CDPStructuredEventV1Endpoint structuredEventsV1Endpoint() {
        return getEndpoint(CDPStructuredEventV1Endpoint.class);
    }

    default AuthorizationUtilEndpoint authorizationUtilEndpoint() {
        return getEndpoint(AuthorizationUtilEndpoint.class);
    }

    default AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint() {
        return getEndpoint(AzureMarketplaceTermsEndpoint.class);
    }

    default TermsEndpoint termsEndpoint() {
        return getEndpoint(TermsEndpoint.class);
    }

    default EncryptionProfileEndpoint encryptionProfileEndpoint() {
        return getEndpoint(EncryptionProfileEndpoint.class);
    }

    default EnvironmentHybridEndpoint hybridEndpoint() {
        return getEndpoint(EnvironmentHybridEndpoint.class);
    }

    default CrossRealmTrustEndpoint crossRealmTrustEndpoint() {
        return getEndpoint(CrossRealmTrustEndpoint.class);
    }
}
