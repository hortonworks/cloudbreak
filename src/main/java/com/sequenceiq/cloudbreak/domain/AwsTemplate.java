package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.VolumeType;

@Entity
public class AwsTemplate extends Template implements ProvisionEntity {

    @Column(nullable = false)
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private Regions region;
    private String amiId;
    @Enumerated(EnumType.STRING)
    private InstanceType instanceType;
    private String sshLocation;
    private VolumeType volumeType;
    private Boolean spotPriced;

    @ManyToOne
    @JoinColumn(name = "awsTemplate_awsTemplateOwner")
    private User awsTemplateOwner;

    public AwsTemplate() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Override
    public User getOwner() {
        return awsTemplateOwner;
    }

    public VolumeType getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(VolumeType volumeType) {
        this.volumeType = volumeType;
    }

    public Boolean isSpotPriced() {
        return spotPriced;
    }

    public void setSpotPriced(Boolean spotPriced) {
        this.spotPriced = spotPriced;
    }

}
