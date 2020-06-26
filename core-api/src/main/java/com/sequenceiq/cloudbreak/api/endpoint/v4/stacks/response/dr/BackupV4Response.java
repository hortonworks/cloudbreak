package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class BackupV4Response {
    private FlowIdentifier flowIdentifier;

    public BackupV4Response() {
    }

    public BackupV4Response(FlowIdentifier flowIdentifier) {
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
        return "BackupV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
