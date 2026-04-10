package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxConfigStalenessResponse {

    @Schema(description = ModelDescriptions.ClusterModelDescription.CONFIG_STALENESS_STATE)
    private String state;

    @Schema(description = ModelDescriptions.ClusterModelDescription.CONFIG_STALENESS_DETAILS)
    private String details;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "SdxConfigStalenessResponse{" +
                "state='" + state + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
