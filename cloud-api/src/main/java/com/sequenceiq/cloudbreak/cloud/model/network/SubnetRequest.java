package com.sequenceiq.cloudbreak.cloud.model.network;

public class SubnetRequest {

    private String publicSubnetCidr;

    private String privateSubnetCidr;

    private String availabilityZone;

    private int subnetGroup;

    private int index;

    private SubnetType type;

    public String getPublicSubnetCidr() {
        return publicSubnetCidr;
    }

    public void setPublicSubnetCidr(String publicSubnetCidr) {
        this.publicSubnetCidr = publicSubnetCidr;
    }

    public String getPrivateSubnetCidr() {
        return privateSubnetCidr;
    }

    public void setPrivateSubnetCidr(String privateSubnetCidr) {
        this.privateSubnetCidr = privateSubnetCidr;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public int getSubnetGroup() {
        return subnetGroup;
    }

    public void setSubnetGroup(int subnetGroup) {
        this.subnetGroup = subnetGroup;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setType(SubnetType type) {
        this.type = type;
    }

    public SubnetType getType() {
        return type;
    }
}
