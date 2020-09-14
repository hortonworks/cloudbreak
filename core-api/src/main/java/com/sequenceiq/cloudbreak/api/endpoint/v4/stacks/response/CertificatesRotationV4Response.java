package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class CertificatesRotationV4Response {
    private FlowIdentifier flowIdentifier;

    public CertificatesRotationV4Response() {
    }

    public CertificatesRotationV4Response(FlowIdentifier flowIdentifier) {
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
        return "CertificateRotationV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
