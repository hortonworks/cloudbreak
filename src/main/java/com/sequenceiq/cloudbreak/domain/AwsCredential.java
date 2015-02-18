package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

@Entity
public class AwsCredential extends Credential implements ProvisionEntity {

    private String roleArn;

    private String keyPairName;

    @OneToOne
    private TemporaryAwsCredentials temporaryAwsCredentials;

    public AwsCredential() {

    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public void setKeyPairName(String keyPairName) {
        this.keyPairName = keyPairName;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public TemporaryAwsCredentials getTemporaryAwsCredentials() {
        return temporaryAwsCredentials;
    }

    public void setTemporaryAwsCredentials(TemporaryAwsCredentials temporaryAwsCredentials) {
        this.temporaryAwsCredentials = temporaryAwsCredentials;
    }

}
