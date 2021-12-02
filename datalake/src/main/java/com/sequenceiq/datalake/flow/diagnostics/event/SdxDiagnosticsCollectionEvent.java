package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;
import java.util.Objects;

import com.sequenceiq.datalake.flow.SdxEvent;
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

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxDiagnosticsCollectionEvent.class, other,
                event -> Objects.equals(flowIdentifier, event.flowIdentifier)
                        && Objects.equals(getProperties(), event.getProperties()));
    }
}
