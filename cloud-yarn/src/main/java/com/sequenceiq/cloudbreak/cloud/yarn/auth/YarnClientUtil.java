package com.sequenceiq.cloudbreak.cloud.yarn.auth;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnHttpClient;

@Component
public class YarnClientUtil {
    public YarnClient createYarnClient(AuthenticatedContext authenticatedContext) {
        YarnCredentialView yarnCredentialView = new YarnCredentialView(authenticatedContext.getCloudCredential());
        return new YarnHttpClient(yarnCredentialView.getYarnEndpoint());
    }
}
