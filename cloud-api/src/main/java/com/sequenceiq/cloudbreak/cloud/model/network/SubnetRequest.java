package com.sequenceiq.cloudbreak.cloud.model.network;

public class SubnetRequest {

    private String publicSubnetCidr;

    private String privateSubnetCidr;

    private String availabilityZone;

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
}
