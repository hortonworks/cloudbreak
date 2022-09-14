package com.sequenceiq.cloudbreak.service.cost.model;

import java.util.List;

public class ClusterCostDto {

    private String region;

    public List<InstanceGroupCostDto> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupCostDto> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    private List<InstanceGroupCostDto> instanceGroups;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

}
