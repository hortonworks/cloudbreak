package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class AzureCredential extends Credential implements ProvisionEntity {

    private static final int END_INDEX = 24;
    private String subscriptionId;

    private String jks;

    private String postFix;

    @ManyToOne
    @JoinColumn(name = "azureCredential_azureCredentialOwner")
    private User azureCredentialOwner;

    @Column(nullable = false)
    private String name;

    public AzureCredential() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public User getAzureCredentialOwner() {
        return azureCredentialOwner;
    }

    public void setAzureCredentialOwner(User azureCredentialOwner) {
        this.azureCredentialOwner = azureCredentialOwner;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public User getOwner() {
        return azureCredentialOwner;
    }

    @Override
    public String getCredentialName() {
        return name;
    }

    public String getCommonName() {
        String result = name.replaceAll("\\s+", "") + postFix;
        if (result.length() > END_INDEX) {
            return result.substring(result.length() - END_INDEX, result.length());
        }
        return result;
    }
}
