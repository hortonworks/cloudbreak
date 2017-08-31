package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AzureCredentialView {

    private CloudCredential cloudCredential;

    public AzureCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public Long getId() {
        return cloudCredential.getId();
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

}
