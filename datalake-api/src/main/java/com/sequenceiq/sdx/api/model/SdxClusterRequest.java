package com.sequenceiq.sdx.api.model;

import java.util.Map;

import javax.validation.constraints.NotNull;

public class SdxClusterRequest {

    @NotNull
    private String environment;

    @NotNull
    private String accessCidr;

    // what does it contain?
    @NotNull
    private String clusterShape;

    private Map<String, String> tags;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getAccessCidr() {
        return accessCidr;
    }

    public void setAccessCidr(String accessCidr) {
        this.accessCidr = accessCidr;
    }

    public String getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(String clusterShape) {
        this.clusterShape = clusterShape;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
