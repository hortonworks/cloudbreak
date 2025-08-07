package com.sequenceiq.environment.client;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.AuditCredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentDefaultComputeClusterEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.terms.endpoint.TermsEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public interface EnvironmentClient {
    CredentialEndpoint credentialV1Endpoint();

    AuditCredentialEndpoint auditCredentialV1Endpoint();

    ProxyEndpoint proxyV1Endpoint();

    EnvironmentEndpoint environmentV1Endpoint();

    EnvironmentDefaultComputeClusterEndpoint defaultComputeClusterEndpoint();

    FlowEndpoint flowEndpoint();

    FlowPublicEndpoint flowPublicEndpoint();

    CDPStructuredEventV1Endpoint structuredEventsV1Endpoint();

    AuthorizationUtilEndpoint authorizationUtilEndpoint();

    AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint();

    TermsEndpoint termsEndpoint();

    EncryptionProfileEndpoint encryptionProfileEndpoint();
}