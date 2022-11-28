package com.sequenceiq.cloudbreak.cloud.model;

public class CloudCredentialSettings {

    private boolean verifyPermissions;

    private boolean skipOrgPolicyDecisions;

    public CloudCredentialSettings() {
    }

    public CloudCredentialSettings(boolean verifyPermissions, boolean skipOrgPolicyDecisions) {
        this.verifyPermissions = verifyPermissions;
        this.skipOrgPolicyDecisions = skipOrgPolicyDecisions;
    }

    public boolean isVerifyPermissions() {
        return verifyPermissions;
    }

    public void setVerifyPermissions(boolean verifyPermissions) {
        this.verifyPermissions = verifyPermissions;
    }

    public boolean isSkipOrgPolicyDecisions() {
        return skipOrgPolicyDecisions;
    }

    public void setSkipOrgPolicyDecisions(boolean skipOrgPolicyDecisions) {
        this.skipOrgPolicyDecisions = skipOrgPolicyDecisions;
    }

    @Override
    public String toString() {
        return "CloudVerifyPermissions{" +
                "verifyPermissions=" + verifyPermissions +
                ", skipOrgPolicyDecisions=" + skipOrgPolicyDecisions +
                '}';
    }
}
