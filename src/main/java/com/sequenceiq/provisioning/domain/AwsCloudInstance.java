package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
public class AwsCloudInstance implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cloudinstance_generator")
    @SequenceGenerator(name = "cloudinstance_generator", sequenceName = "cloudsequence_table")
    private Long id;

    private Integer clusterSize;

    @OneToOne
    private AwsInfra awsInfra;

    @ManyToOne
    private User user;

    public AwsCloudInstance() {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
