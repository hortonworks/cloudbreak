package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

@Entity
public class AzureCredential extends Credential implements ProvisionEntity {

    private static final int END_INDEX = 24;
    private String subscriptionId;

    private String jks;

    private String postFix;

    public AzureCredential() {

    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getJks() {
        return jks;
    }

    public void setJks(String jks) {
        this.jks = jks;
    }

    public String getPostFix() {
        return postFix;
    }

    public void setPostFix(String postFix) {
        this.postFix = postFix;
    }

    public String getCommonName() {
        String result = getName().replaceAll("\\s+", "") + postFix;
        if (result.length() > END_INDEX) {
            return result.substring(result.length() - END_INDEX, result.length());
        }
        return result;
    }
}
