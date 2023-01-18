package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxSyncComponentVersionsFromCmResponse {

    @Schema(description = ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    public SdxSyncComponentVersionsFromCmResponse() {
    }

    public SdxSyncComponentVersionsFromCmResponse(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "SdxSyncComponentVersionsFromCmResponse{" + "flowIdentifier=" + flowIdentifier + '}';
    }
}
