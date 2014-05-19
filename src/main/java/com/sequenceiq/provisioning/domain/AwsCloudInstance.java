package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class AwsCloudInstance implements CloudInstance, ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer clusterSize;

    @OneToOne
    private AwsInfra awsInfra;

    public AwsCloudInstance() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    @Override
    public CloudPlatform getPlatform() {
        return CloudPlatform.AWS;
    }


}
