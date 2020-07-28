package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxDiagnosticsCollectionEvent extends BaseSdxDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    public SdxDiagnosticsCollectionEvent(Long sdxId, String userId, Map<String, Object> properties, FlowIdentifier flowIdentifier) {
        super(sdxId, userId, properties);
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }
}
