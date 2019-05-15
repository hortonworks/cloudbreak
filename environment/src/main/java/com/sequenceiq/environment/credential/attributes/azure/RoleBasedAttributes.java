package com.sequenceiq.environment.credential.attributes.azure;

public class RoleBasedAttributes {

    private String roleName;

    private String deploymentAddress;

    private String spDisplayName;

    private Boolean codeGrantFlow;

    private String appObjectId;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDeploymentAddress() {
        return deploymentAddress;
    }

    public void setDeploymentAddress(String deploymentAddress) {
        this.deploymentAddress = deploymentAddress;
    }

    public String getSpDisplayName() {
        return spDisplayName;
    }

    public void setSpDisplayName(String spDisplayName) {
        this.spDisplayName = spDisplayName;
    }

    public Boolean getCodeGrantFlow() {
        return codeGrantFlow;
    }

    public void setCodeGrantFlow(Boolean codeGrantFlow) {
        this.codeGrantFlow = codeGrantFlow;
    }

    public String getAppObjectId() {
        return appObjectId;
    }

    public void setAppObjectId(String appObjectId) {
        this.appObjectId = appObjectId;
    }
}
