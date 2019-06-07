package com.sequenceiq.cloudbreak.cloud.model;

public class CloudSubnet {

    private String id;

    private String name;

    private String availabilityZone;

    public CloudSubnet() {
    }

    public CloudSubnet(String id, String name) {
        this.id = id;
        this.name = name;
        this.availabilityZone = null;
    }

    public CloudSubnet(String id, String name, String availabilityZone) {
        this.id = id;
        this.name = name;
        this.availabilityZone = availabilityZone;
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
}
