package com.sequenceiq.environment.credential.attributes.aws;

public class RoleBasedCredentialAttributes {

    private String roleArn;

    private String externalId;

    private String roleAssumeType;

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getRoleAssumeType() {
        return roleAssumeType;
    }

    public void setRoleAssumeType(String roleAssumeType) {
        this.roleAssumeType = roleAssumeType;
    }
}
