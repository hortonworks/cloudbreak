package com.sequenceiq.cloudbreak.cloud.cumulus.yarn.auth;

import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.CumulusYarnConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class CumulusYarnCredentialView {
    private final CloudCredential cloudCredential;

    public CumulusYarnCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getYarnEndpoint() {
        return cloudCredential.getParameter(CumulusYarnConstants.CUMULUS_YARN_ENDPOINT_PARAMETER, String.class);
    }
}
