package com.sequenceiq.common.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UsedSubnetWithResourceResponse {

    private String name;

    private String subnetId;

    private String resourceCrn;

    private String type;

    @JsonCreator
    public UsedSubnetWithResourceResponse(
            @JsonProperty("name") String name,
            @JsonProperty("subnetId") String subnetId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("type") String type) {
        this.name = name;
        this.subnetId = subnetId;
        this.resourceCrn = resourceCrn;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "UsedSubnetWithResourceResponse{" +
                "name='" + name + '\'' +
                ", subnetId='" + subnetId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
