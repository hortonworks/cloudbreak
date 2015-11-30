package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

//@Entity
public class AwsTemplate extends Template implements ProvisionEntity {

    private String sshLocation;
    private String volumeType;
    private Double spotPrice;

    public AwsTemplate() {
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

    public String getVolumeTypeName() {
        return getVolumeType();
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public Double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }

    public boolean isEphemeral() {
        return "ephemeral".equals(volumeType);
    }
}
