package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class AwsTemplate extends Template implements ProvisionEntity {

    @Enumerated(EnumType.STRING)
    private AwsInstanceType instanceType;
    private String sshLocation;
    @Enumerated(EnumType.STRING)
    private AwsVolumeType volumeType;
    private Double spotPrice;
    @Enumerated(EnumType.STRING)
    private AwsEncryption encrypted;

    public AwsTemplate() {
    }

    public AwsInstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(AwsInstanceType instanceType) {
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

    public AwsVolumeType getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(AwsVolumeType volumeType) {
        this.volumeType = volumeType;
    }

    public Double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }

    public AwsEncryption getEncrypted() {
        return encrypted;
    }

    public boolean isEncrypted() {
        return AwsEncryption.TRUE.equals(encrypted) ? true : false;
    }

    public void setEncrypted(AwsEncryption encrypted) {
        this.encrypted = encrypted;
    }
}
