package com.sequenceiq.environment.api.v1.platformresource.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformResourceGroupResponse {

    private String name;

    public PlatformResourceGroupResponse() {
    }

    public PlatformResourceGroupResponse(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PlatformResourceGroupResponse{" +
                "name='" + name + '\'' +
                '}';
    }
}
