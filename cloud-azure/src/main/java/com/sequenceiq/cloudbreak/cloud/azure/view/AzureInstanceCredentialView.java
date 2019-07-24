package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class AzureInstanceCredentialView {

    private final CloudStack cloudStack;

    public AzureInstanceCredentialView(CloudStack cloudStack) {
        this.cloudStack = cloudStack;
    }

    public String getPublicKey() {
        return cloudStack.getInstanceAuthentication().getPublicKey().trim();
    }

    public boolean passwordAuthenticationRequired() {
        return cloudStack.getInstanceAuthentication().getPublicKey().startsWith("Basic: ");
    }

    public String getPassword() {
        return cloudStack.getInstanceAuthentication().getPublicKey().replaceAll("Basic: ", "");
    }

    public String getLoginUserName() {
        return cloudStack.getInstanceAuthentication().getLoginUserName();
    }
}
