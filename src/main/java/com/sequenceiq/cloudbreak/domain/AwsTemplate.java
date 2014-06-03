package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;

@Entity
public class AwsTemplate extends Template implements ProvisionEntity {

    private String name;
    private Regions region;
    private String keyName;
    private String amiId;
    private InstanceType instanceType;
    private String sshLocation;

    @ManyToOne
    private User awsTemplateOwner;

    public AwsTemplate() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    public InstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(InstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public String getSshLocation() {
        return sshLocation;
    }

    public void setSshLocation(String sshLocation) {
        this.sshLocation = sshLocation;
    }

    public User getAwsTemplateOwner() {
        return awsTemplateOwner;
    }

    public void setAwsTemplateOwner(User awsTemplateOwner) {
        this.awsTemplateOwner = awsTemplateOwner;
    }

    @Override
    public void setUser(User user) {
        this.awsTemplateOwner = user;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }
}
