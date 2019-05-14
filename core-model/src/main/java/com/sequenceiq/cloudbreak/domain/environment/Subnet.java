package com.sequenceiq.cloudbreak.domain.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Subnet {

    private String subnetId;

    private String cidr;

    private SubnetVisibility visibility;

    private String availabilityZone;

    public Subnet(String subnetId) {
        this.subnetId = subnetId;
    }

    public Subnet() {
    }

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

    public SubnetVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(SubnetVisibility visibility) {
        this.visibility = visibility;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }
}
