package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class RecoveryV4Response {

    private FlowIdentifier flowIdentifier;

    public RecoveryV4Response() {
    }

    public RecoveryV4Response(FlowIdentifier flowIdentifier) {
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
        return "RecoveryV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
