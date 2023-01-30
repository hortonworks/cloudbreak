package com.sequenceiq.cloudbreak.cost.model;

import java.util.List;
import java.util.Locale;

public class ClusterCostDto {

    private String region;

    private String status;

    private List<InstanceGroupCostDto> instanceGroups;

    public List<InstanceGroupCostDto> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupCostDto> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region.toLowerCase(Locale.ROOT).replaceAll(" ", "");
    }

    public boolean isComputeRunning() {
        return !status.startsWith("STOPPED");
    }

    @Override
    public String toString() {
        return "ClusterCostDto{" +
                "region='" + region + '\'' +
                ", status='" + status + '\'' +
                ", instanceGroups=" + instanceGroups +
                '}';
    }
}
