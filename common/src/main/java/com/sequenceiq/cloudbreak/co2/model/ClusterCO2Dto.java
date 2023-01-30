package com.sequenceiq.cloudbreak.co2.model;

import java.util.List;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class ClusterCO2Dto {

    private CloudPlatform cloudPlatform;

    private String region;

    private String status;

    private List<InstanceGroupCO2Dto> instanceGroups;

    public boolean isComputeRunning() {
        return !status.startsWith("STOPPED") && !status.startsWith("DELETED");
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<InstanceGroupCO2Dto> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupCO2Dto> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    @Override
    public String toString() {
        return "ClusterCO2Dto{" +
                "cloudPlatform=" + cloudPlatform +
                ", region='" + region + '\'' +
                ", status='" + status + '\'' +
                ", instanceGroups=" + instanceGroups +
                '}';
    }
}
