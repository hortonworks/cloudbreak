package com.sequenceiq.cloudbreak.cloud.openstack;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;

@Component
public class OpenStackCredentialConnector implements CredentialConnector {

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public CloudCredentialStatus verify(@Nonnull AuthenticatedContext authenticatedContext, CredentialVerificationContext credentialVerificationContext) {
        // TODO: check instance types or something like this
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(@Nonnull AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(@Nonnull AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }
}
