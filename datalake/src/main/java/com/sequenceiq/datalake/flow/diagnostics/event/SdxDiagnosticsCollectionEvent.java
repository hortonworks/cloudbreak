package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxDiagnosticsCollectionEvent extends BaseSdxDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public SdxDiagnosticsCollectionEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonTypeInfo(use = CLASS, property = "@type") @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
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
