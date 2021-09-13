package com.sequenceiq.environment.credential.attributes.azure;

public class AzureCredentialAttributes {

    private String subscriptionId;

    private String tenantId;

    private AppBasedAttributes appBased;

    private AppBasedAttributes lightHouseBased;

    private CodeGrantFlowAttributes codeGrantFlowBased;

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

    public AppBasedAttributes getAppBased() {
        return appBased;
    }

    public void setAppBased(AppBasedAttributes appBased) {
        this.appBased = appBased;
    }

    public CodeGrantFlowAttributes getCodeGrantFlowBased() {
        return codeGrantFlowBased;
    }

    public void setCodeGrantFlowBased(CodeGrantFlowAttributes codeGrantFlowBased) {
        this.codeGrantFlowBased = codeGrantFlowBased;
    }

    public AppBasedAttributes getLightHouseBased() {
        return lightHouseBased;
    }

    public void setLightHouseBased(AppBasedAttributes lightHouseBased) {
        this.lightHouseBased = lightHouseBased;
    }
}
