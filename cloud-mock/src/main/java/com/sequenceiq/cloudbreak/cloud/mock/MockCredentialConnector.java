package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

@Service
public class MockCredentialConnector implements CredentialConnector {
    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        return new CloudCredentialStatus(credential, CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public Map<String, String> interactiveLogin(AuthenticatedContext authenticatedContext, ExtendedCloudCredential extendedCloudCredential) {
        throw new UnsupportedOperationException("Interactive login not supported on MOCK");
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }
}
