package com.sequenceiq.cloudbreak.cloud.yarn.auth;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.yarn.YarnConstants;

public class YarnCredentialView {
    private final CloudCredential cloudCredential;

    public YarnCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getYarnEndpoint() {
        return cloudCredential.getParameter(YarnConstants.YARN_ENDPOINT_PARAMETER, String.class);
    }
}
