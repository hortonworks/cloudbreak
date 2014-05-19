package com.sequenceiq.provisioning.controller.json;

import javax.validation.constraints.Min;

import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.json.JsonEntity;

public class CloudInstanceRequest implements JsonEntity {

    @Min(value = 2)
    private int clusterSize;
    private String infraId;
    private CloudPlatform cloudPlatform;

    public CloudInstanceRequest() {
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(int clusterSize) {
        this.clusterSize = clusterSize;
    }

    public String getInfraId() {
        return infraId;
    }

    public void setInfraId(String infraId) {
        this.infraId = infraId;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }
}
