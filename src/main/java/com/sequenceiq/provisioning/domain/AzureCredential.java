package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class AzureCredential extends Credential implements ProvisionEntity {

    private String subscriptionId;

    private String jks;

    @ManyToOne
    private User azureCredentialOwner;

    public AzureCredential() {

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
}
