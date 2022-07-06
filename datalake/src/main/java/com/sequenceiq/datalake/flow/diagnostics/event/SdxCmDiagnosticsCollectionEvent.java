package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxCmDiagnosticsCollectionEvent extends BaseSdxCmDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public SdxCmDiagnosticsCollectionEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        super(sdxId, userId, properties);
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxCmDiagnosticsCollectionEvent.class, other,
                event -> Objects.equals(flowIdentifier, event.flowIdentifier)
                        && Objects.equals(getProperties(), event.getProperties()));
    }
}
