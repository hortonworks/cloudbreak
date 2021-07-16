package com.sequenceiq.sdx.api.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxRecoveryResponse {

    private String reason;

    private FlowIdentifier flowIdentifier;

    public SdxRecoveryResponse(String reason, FlowIdentifier flowIdentifier) {
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }
}
