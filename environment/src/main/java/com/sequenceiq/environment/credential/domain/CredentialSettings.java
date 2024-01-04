package com.sequenceiq.environment.credential.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CredentialSettings implements Serializable {

    @Column(name = "verifypermissions")
    private boolean verifyPermissions;

    @Column(name = "skiporgpolicydecisions")
    private boolean skipOrgPolicyDecisions;

    public CredentialSettings() {
    }

    public CredentialSettings(boolean verifyPermissions, boolean skipOrgPolicyDecisions) {
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
        return "CredentialSettings{" +
                "verifyPermissions=" + verifyPermissions +
                ", skipOrgPolicyDecisions=" + skipOrgPolicyDecisions +
                '}';
    }
}
