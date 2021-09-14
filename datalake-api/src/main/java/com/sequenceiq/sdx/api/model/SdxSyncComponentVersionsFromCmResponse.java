package com.sequenceiq.sdx.api.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxSyncComponentVersionsFromCmResponse {

    private String reason;

    private FlowIdentifier flowIdentifier;

    public SdxSyncComponentVersionsFromCmResponse() {
    }

    public SdxSyncComponentVersionsFromCmResponse(String reason, FlowIdentifier flowIdentifier) {
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public SdxSyncComponentVersionsFromCmResponse(String reason) {
        this.reason = reason;
        this.flowIdentifier = FlowIdentifier.notTriggered();
    }

    public String getReason() {
        return reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "SdxSyncCmResponse{" +
                "reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
