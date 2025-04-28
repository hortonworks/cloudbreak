package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModifySeLinuxResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public ModifySeLinuxResponse(@JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "SetSeLinuxToEnforcingResponse {flowIdentifier=" + flowIdentifier.toString() + "}";
    }
}
