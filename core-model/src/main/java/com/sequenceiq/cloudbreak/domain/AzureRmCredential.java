package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
public class AzureRmCredential extends Credential implements ProvisionEntity {

    private String subscriptionId;
    private String tenantId;
    private String accesKey;
    private String secretKey;

    public AzureRmCredential() {

    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE_RM;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAccesKey() {
        return accesKey;
    }

    public void setAccesKey(String accesKey) {
        this.accesKey = accesKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
