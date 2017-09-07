package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AzureCredentialView {

    private final CloudCredential cloudCredential;

    public AzureCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public Long getId() {
        return cloudCredential.getId();
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

    public String getRoleName() {
        return cloudCredential.getParameter("roleName", String.class);
    }

    public String getRoleType() {
        return cloudCredential.getParameter("roleType", String.class);
    }

    public boolean passwordAuthenticationRequired() {
        return cloudCredential.getPublicKey().startsWith("Basic: ");
    }

    public String getPassword() {
        return cloudCredential.getPublicKey().replaceAll("Basic: ", "");
    }

    public String getLoginUserName() {
        return cloudCredential.getLoginUserName();
    }

}
