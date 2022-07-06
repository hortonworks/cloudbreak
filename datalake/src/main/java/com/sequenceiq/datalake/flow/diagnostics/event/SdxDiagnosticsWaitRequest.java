package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxDiagnosticsWaitRequest extends BaseSdxDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public SdxDiagnosticsWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        super(sdxId, userId, properties);
        this.flowIdentifier = flowIdentifier;
    }

    public static SdxDiagnosticsWaitRequest from(SdxContext context, SdxDiagnosticsCollectionEvent event) {
        return new SdxDiagnosticsWaitRequest(context.getSdxId(), context.getUserId(), event.getProperties(), event.getFlowIdentifier());
    }

    @Override
    public String selector() {
        return "SdxDiagnosticsWaitRequest";
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }
}
