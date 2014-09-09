package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.VolumeType;

@Entity
public class AwsTemplate extends Template implements ProvisionEntity {

    @Enumerated(EnumType.STRING)
    private Regions region;
    private String amiId;
    @Enumerated(EnumType.STRING)
    private InstanceType instanceType;
    private String sshLocation;
    @Enumerated(EnumType.STRING)
    private VolumeType volumeType;
    private Double spotPrice;

    public AwsTemplate() {
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

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    public VolumeType getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(VolumeType volumeType) {
        this.volumeType = volumeType;
    }

    public Double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }

}
