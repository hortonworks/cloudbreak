package com.sequenceiq.cloudbreak.cloud.yarn.auth;

import java.util.Locale;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.yarn.YarnConstants;

public class YarnCredentialView {

    private final CloudCredential cloudCredential;

    public YarnCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getYarnEndpoint() {
        String yarn = YarnConstants.YARN_PLATFORM.value().toLowerCase(Locale.ROOT);
        if (cloudCredential.hasParameter(yarn)) {
            return (String) cloudCredential.getParameter(yarn, Map.class).get(YarnConstants.YARN_ENDPOINT_PARAMETER);
        }
        return cloudCredential.getParameter(YarnConstants.YARN_ENDPOINT_PARAMETER, String.class);
    }
}
