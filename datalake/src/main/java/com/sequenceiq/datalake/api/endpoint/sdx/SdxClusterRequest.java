package com.sequenceiq.datalake.api.endpoint.sdx;

import java.util.Map;

public class SdxClusterRequest {

    private String accessCidr;

    // what does it contain?
    private String clusterShape;

    private Map<String, String> tags;

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
