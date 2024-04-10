package com.sequenceiq.externalizedcompute.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalizedComputeClusterInternalRequest extends ExternalizedComputeClusterRequest {

    @Schema
    private boolean defaultCluster;

    public boolean isDefaultCluster() {
        return defaultCluster;
    }

    public void setDefaultCluster(boolean defaultCluster) {
        this.defaultCluster = defaultCluster;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterInternalRequest{" +
                "defaultCluster=" + defaultCluster +
                "} " + super.toString();
    }
}
