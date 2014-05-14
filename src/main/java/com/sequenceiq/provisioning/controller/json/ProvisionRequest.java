package com.sequenceiq.provisioning.controller.json;

import com.amazonaws.regions.Regions;
import com.sequenceiq.provisioning.domain.CloudPlatform;

public class ProvisionRequest {

    private CloudPlatform cloudPlatform;
    private String clusterName;
    private int clusterSize;
    private Regions region;
    private String keyName;
    private String roleArn;

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform type) {
        this.cloudPlatform = type;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(int clusterSize) {
        this.clusterSize = clusterSize;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

}
