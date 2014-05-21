package com.sequenceiq.provisioning.controller.json;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.provisioning.domain.CloudPlatform;

public class CloudInstanceJson implements JsonEntity {

    private Long id;
    @Min(value = 2)
    private int clusterSize;
    private String cloudName;
    private Long infraId;
    private CloudPlatform cloudPlatform;

    public CloudInstanceJson() {
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(int clusterSize) {
        this.clusterSize = clusterSize;
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public Long getInfraId() {
        return infraId;
    }

    public void setInfraId(Long infraId) {
        this.infraId = infraId;
    }

    @JsonProperty("cloudPlatform")
    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    @JsonIgnore
    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }
}
