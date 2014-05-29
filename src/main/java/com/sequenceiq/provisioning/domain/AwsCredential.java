package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class AwsCredential extends Credential implements ProvisionEntity {

    private String roleArn;

    @ManyToOne
    private User awsCredentialOwner;

    public AwsCredential() {

    }

    public User getAwsCredentialOwner() {
        return awsCredentialOwner;
    }

    public void setAwsCredentialOwner(User awsCredentialOwner) {
        this.awsCredentialOwner = awsCredentialOwner;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

}
