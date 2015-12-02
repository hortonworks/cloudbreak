package com.sequenceiq.cloudbreak.cloud.arm.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class ArmCredentialView {

    private CloudCredential cloudCredential;

    public ArmCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getPublicKey() {
        return cloudCredential.getPublicKey();
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getSubscriptionId() {
        return cloudCredential.getParameter("subscriptionId", String.class);
    }

    public String getAccessKey() {
        return cloudCredential.getParameter("accessKey", String.class);
    }

    public String getSecretKey() {
        return cloudCredential.getParameter("secretKey", String.class);
    }

    public String getTenantId() {
        return cloudCredential.getParameter("tenantId", String.class);
    }

    public boolean passwordAuthenticationRequired() {
        return cloudCredential.getPublicKey().startsWith("Basic: ");
    }

    public String getPassword() {
        return cloudCredential.getPublicKey().replaceAll("Basic: ", "");
    }
}
