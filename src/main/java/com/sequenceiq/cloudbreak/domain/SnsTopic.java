package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.amazonaws.regions.Regions;

@Entity
public class SnsTopic implements ProvisionEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String topicArn;
    private Regions region;

    @ManyToOne
    private AwsCredential credential;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public AwsCredential getCredential() {
        return credential;
    }

    public void setCredential(AwsCredential credential) {
        this.credential = credential;
    }

}
