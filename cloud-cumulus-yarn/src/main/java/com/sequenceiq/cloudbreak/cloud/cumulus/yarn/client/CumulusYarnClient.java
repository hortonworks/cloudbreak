package com.sequenceiq.cloudbreak.cloud.cumulus.yarn.client;

import org.apache.cb.yarn.service.api.ApiClient;
import org.apache.cb.yarn.service.api.impl.DefaultApi;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.auth.CumulusYarnCredentialView;

@Component
public class CumulusYarnClient {
    public DefaultApi createApi(AuthenticatedContext authenticatedContext) {
        CumulusYarnCredentialView credentialView = new CumulusYarnCredentialView(authenticatedContext.getCloudCredential());
        ApiClient apiClient = new ApiClient().setBasePath(credentialView.getYarnEndpoint());
        return new DefaultApi(apiClient);
    }
}
