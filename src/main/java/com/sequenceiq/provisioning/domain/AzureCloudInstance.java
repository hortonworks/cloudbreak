package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
public class AzureCloudInstance implements ProvisionEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cloudinstance_generator")
    @SequenceGenerator(name = "cloudinstance_generator", sequenceName = "cloudsequence_table")
    private Long id;

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


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
