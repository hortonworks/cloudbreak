package com.sequenceiq.environment.credential.attributes.azure;

public class AzureCredentialAttributes {

    private String subscriptionId;

    private String tenantId;

    private String accessKey;

    private AppBasedAttributes appBased;

    private RoleBasedAttributes roleBased;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public AppBasedAttributes getAppBased() {
        return appBased;
    }

    public void setAppBased(AppBasedAttributes appBased) {
        this.appBased = appBased;
    }

    public RoleBasedAttributes getRoleBased() {
        return roleBased;
    }

    public void setRoleBased(RoleBasedAttributes roleBased) {
        this.roleBased = roleBased;
    }
}
