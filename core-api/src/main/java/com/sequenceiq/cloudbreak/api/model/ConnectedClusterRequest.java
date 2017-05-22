package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectedClusterRequest {

    private Long sourceClusterId;

    private String sourceClusterName;

    public Long getSourceClusterId() {
        return sourceClusterId;
    }

    public void setSourceClusterId(Long sourceClusterId) {
        this.sourceClusterId = sourceClusterId;
    }

    public String getSourceClusterName() {
        return sourceClusterName;
    }

    public void setSourceClusterName(String sourceClusterName) {
        this.sourceClusterName = sourceClusterName;
    }
}
