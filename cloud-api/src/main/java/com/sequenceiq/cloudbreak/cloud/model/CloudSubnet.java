package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;

public class CloudSubnet implements Serializable {

    private String id;

    private String name;

    private String availabilityZone;

    private String cidr;

    private boolean privateSubnet;

    public CloudSubnet() {
    }

    public CloudSubnet(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public CloudSubnet(String id, String name, String availabilityZone, String cidr) {
        this.id = id;
        this.name = name;
        this.availabilityZone = availabilityZone;
        this.cidr = cidr;
    }

    public CloudSubnet(String id, String name, String availabilityZone, String cidr, boolean privateSubnet) {
        this.id = id;
        this.name = name;
        this.availabilityZone = availabilityZone;
        this.cidr = cidr;
        this.privateSubnet = privateSubnet;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
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

    @Override
    public String toString() {
        return "CloudSubnet{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", availabilityZone='" + availabilityZone + '\''
                + ", cidr='" + cidr + '\''
                + '}';
    }
}
