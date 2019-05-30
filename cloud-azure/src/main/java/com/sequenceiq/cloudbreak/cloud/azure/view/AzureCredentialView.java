package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AzureCredentialView {

    private final CloudCredential cloudCredential;

    public AzureCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getCredentialCrn() {
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

    public Boolean getCodeGrantFlow() {
        return cloudCredential.getParameter("codeGrantFlow", Boolean.class);
    }

    public String getAppLoginUrl() {
        return cloudCredential.getParameter("appLoginUrl", String.class);
    }

    public String getCodeGrantFlowState() {
        return cloudCredential.getParameter("codeGrantFlowState", String.class);
    }

    public String getAuthorizationCode() {
        return cloudCredential.getParameter("authorizationCode", String.class);
    }

    public String getRefreshToken() {
        return cloudCredential.getParameter("refreshToken", String.class);
    }

    public String getAppReplyUrl() {
        return cloudCredential.getParameter("appReplyUrl", String.class);
    }

    public String getDeploymentAddress() {
        return cloudCredential.getParameter("deploymentAddress", String.class);
    }
}
