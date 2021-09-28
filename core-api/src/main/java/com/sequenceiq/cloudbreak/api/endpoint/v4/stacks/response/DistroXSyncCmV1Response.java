package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class DistroXSyncCmV1Response {
    private final FlowIdentifier flowIdentifier;

    public DistroXSyncCmV1Response(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "DistroXSyncCmV1Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
