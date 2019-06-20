package com.sequenceiq.cloudbreak.cloud.model;

public class CloudSubnet {

    private String id;

    private String name;

    private String availabilityZone;

    private String cidr;

    public CloudSubnet() {
    }

    public CloudSubnet(String id, String name) {
        this(id, name, null);
    }

    public CloudSubnet(String id, String name, String availabilityZone) {
        this(id, name, availabilityZone, null);
    }

    public CloudSubnet(String id, String name, String availabilityZone, String cidr) {
        this.id = id;
        this.name = name;
        this.availabilityZone = availabilityZone;
        this.cidr = cidr;
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
