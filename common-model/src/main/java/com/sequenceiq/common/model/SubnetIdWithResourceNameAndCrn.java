package com.sequenceiq.common.model;

public class SubnetIdWithResourceNameAndCrn {

    private final String name;

    private final String resourceCrn;

    private final String subnetId;

    private final String type;

    public SubnetIdWithResourceNameAndCrn(String name, String resourceCrn, String subnetId, String type) {
        this.name = name;
        this.resourceCrn = resourceCrn;
        this.subnetId = subnetId;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "SubnetIdWithResourceNameAndCrn{" +
                "name='" + name + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", subnetId='" + subnetId + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
