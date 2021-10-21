package com.sequenceiq.sdx.api.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxRecoveryResponse {

    private FlowIdentifier flowIdentifier;

    public SdxRecoveryResponse() {
    }

    public SdxRecoveryResponse(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "SdxRecoveryResponse{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
