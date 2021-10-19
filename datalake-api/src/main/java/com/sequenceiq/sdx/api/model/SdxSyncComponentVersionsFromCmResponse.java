package com.sequenceiq.sdx.api.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxSyncComponentVersionsFromCmResponse {

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
        return "SdxSyncComponentVersionsFromCmResponse{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
