package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformAccessConfigsResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AccessConfigResponse> accessConfigs = new ArrayList<>();

    public List<AccessConfigResponse> getAccessConfigs() {
        return accessConfigs;
    }

    public void setAccessConfigs(List<AccessConfigResponse> accessConfigs) {
        this.accessConfigs = accessConfigs;
    }

    @Override
    public String toString() {
        return "PlatformAccessConfigsResponse{" +
                "accessConfigs=" + accessConfigs +
                '}';
    }
}
