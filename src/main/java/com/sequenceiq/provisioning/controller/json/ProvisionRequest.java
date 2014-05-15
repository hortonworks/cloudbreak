package com.sequenceiq.provisioning.controller.json;

import java.util.Map;

import javax.validation.constraints.Min;

import com.sequenceiq.provisioning.controller.validation.ValidProvisionRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@ValidProvisionRequest
public class ProvisionRequest {

    private CloudPlatform cloudPlatform;
    private String clusterName;
    @Min(value = 2)
    private int clusterSize;
    private Map<String, String> parameters;

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

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
