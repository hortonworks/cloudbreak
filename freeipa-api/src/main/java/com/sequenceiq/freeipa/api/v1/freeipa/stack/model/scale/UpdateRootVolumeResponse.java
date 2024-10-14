package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class UpdateRootVolumeResponse {

    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public UpdateRootVolumeResponse(@JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "UpdateRootVolumeResponse {flowIdentifier=" + flowIdentifier.toString() + "}";
    }
}
