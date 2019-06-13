package com.sequenceiq.cloudbreak.cloud.model.network;

public class CreatedSubnet {

    private String subnetId;

    private String cidr;

    private boolean privateSubnet;

    private String availabilityZone;

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public boolean isPrivateSubnet() {
        return privateSubnet;
    }

    public void setPrivateSubnet(boolean privateSubnet) {
        this.privateSubnet = privateSubnet;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }
}
