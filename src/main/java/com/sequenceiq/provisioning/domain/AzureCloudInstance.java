package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
public class AzureCloudInstance extends CloudInstance implements ProvisionEntity {

    private Integer clusterSize;

    @OneToOne
    private AzureInfra azureInfra;

    @ManyToOne
    private User user;

    public AzureCloudInstance() {
    }

    public Integer getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(Integer clusterSize) {
        this.clusterSize = clusterSize;
    }

    public AzureInfra getAzureInfra() {
        return azureInfra;
    }

    public void setAzureInfra(AzureInfra azureInfra) {
        this.azureInfra = azureInfra;
    }

    public User getUser() {
        return user;
    }


    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
