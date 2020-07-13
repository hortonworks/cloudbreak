package com.sequenceiq.freeipa.api.v1.diagnostics.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class DiagnosticsCollectionResponse {

    private final FlowIdentifier flowIdentifier;

    public DiagnosticsCollectionResponse(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }
}
