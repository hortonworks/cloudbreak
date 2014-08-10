package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "credentialhistory")
public class CredentialHistory extends AbstractHistory {
    private String dtype;
    private String cloudPlatform;
    @Column(length = 1000000, columnDefinition = "TEXT")
    private String publickey;
    private String keyPairName;
    private String roleArn;
    private String jks;
    private String subscriptionid;
    private String temporaryAccessKeyId;


    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPublickey() {
        return publickey;
    }

    public void setPublickey(String publickey) {
        this.publickey = publickey;
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

    public String getJks() {
        return jks;
    }

    public void setJks(String jks) {
        this.jks = jks;
    }

    public String getSubscriptionid() {
        return subscriptionid;
    }

    public void setSubscriptionid(String subscriptionid) {
        this.subscriptionid = subscriptionid;
    }

    public String getTemporaryAccessKeyId() {
        return temporaryAccessKeyId;
    }

    public void setTemporaryAccessKeyId(String temporaryAccessKeyId) {
        this.temporaryAccessKeyId = temporaryAccessKeyId;
    }
}
