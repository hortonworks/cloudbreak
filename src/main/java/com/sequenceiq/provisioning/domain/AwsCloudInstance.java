package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
public class AwsCloudInstance extends CloudInstance implements ProvisionEntity {

    private Integer clusterSize;

    @OneToOne
    private AwsInfra awsInfra;

    @ManyToOne
    private User user;

    public AwsCloudInstance() {
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    public Integer getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(Integer clusterSize) {
        this.clusterSize = clusterSize;
    }

    public AwsInfra getAwsInfra() {
        return awsInfra;
    }

    public void setAwsInfra(AwsInfra awsInfra) {
        this.awsInfra = awsInfra;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


}
