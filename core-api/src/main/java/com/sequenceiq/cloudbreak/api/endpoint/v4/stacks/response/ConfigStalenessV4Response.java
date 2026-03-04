package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigStalenessV4Response implements JsonEntity {

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
}
