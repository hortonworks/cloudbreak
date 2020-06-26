package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class RestoreV4Response {
    private FlowIdentifier flowIdentifier;

    public RestoreV4Response(FlowIdentifier flowIdentifier) {

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
        return "RestoreV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
