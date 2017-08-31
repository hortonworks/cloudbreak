package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class AzureInstanceCredentialView {

    private CloudStack cloudStack;

    public AzureInstanceCredentialView(CloudStack cloudStack) {
        this.cloudStack = cloudStack;
    }

    public String getPublicKey() {
        return cloudStack.getPublicKey();
    }

    public boolean passwordAuthenticationRequired() {
        return cloudStack.getPublicKey().startsWith("Basic: ");
    }

    public String getPassword() {
        return cloudStack.getPublicKey().replaceAll("Basic: ", "");
    }

    public String getLoginUserName() {
        return cloudStack.getLoginUserName();
    }
}
