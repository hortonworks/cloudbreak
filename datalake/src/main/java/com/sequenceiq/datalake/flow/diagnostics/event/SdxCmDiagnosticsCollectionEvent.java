package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxCmDiagnosticsCollectionEvent extends BaseSdxCmDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    public SdxCmDiagnosticsCollectionEvent(Long sdxId, String userId, Map<String, Object> properties, FlowIdentifier flowIdentifier) {
        super(sdxId, userId, properties);
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }
}
