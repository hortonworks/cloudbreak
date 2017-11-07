package com.sequenceiq.cloudbreak.cloud.yarn;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

@Service
public class YarnCredentialConnector implements CredentialConnector {
    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        return null;
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        return null;
    }

    @Override
    public Map<String, String> interactiveLogin(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential, CredentialNotifier credentialNotifier) {
        return null;
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        return null;
    }
}
