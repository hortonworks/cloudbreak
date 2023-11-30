package com.sequenceiq.externalizedcompute.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalizedComputeClusterResponse extends ExternalizedComputeClusterBase {

    private String liftieClusterName;

    public String getLiftieClusterName() {
        return liftieClusterName;
    }

    public void setLiftieClusterName(String liftieClusterName) {
        this.liftieClusterName = liftieClusterName;
    }
}
