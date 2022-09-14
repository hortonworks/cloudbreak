package com.sequenceiq.cloudbreak.cost.model;

import java.util.List;

public class ClusterCostDto {

    private String region;
    private List<InstanceGroupCostDto> instanceGroups;

    public List<InstanceGroupCostDto> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupCostDto> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "ClusterCostDto{" +
                "region='" + region + '\'' +
                ", instanceGroups=" + instanceGroups +
                '}';
    }
}
