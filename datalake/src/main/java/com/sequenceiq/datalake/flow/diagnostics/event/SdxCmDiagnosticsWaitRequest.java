package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxCmDiagnosticsWaitRequest extends BaseSdxCmDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public SdxCmDiagnosticsWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonTypeInfo(use = CLASS, property = "@type") @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        super(sdxId, userId, properties);
        this.flowIdentifier = flowIdentifier;
    }

    public static SdxDiagnosticsWaitRequest from(SdxContext context, SdxCmDiagnosticsCollectionEvent event) {
        return new SdxDiagnosticsWaitRequest(context.getSdxId(), context.getUserId(), event.getProperties(), event.getFlowIdentifier());
    }

    @Override
    public String selector() {
        return "SdxCmDiagnosticsWaitRequest";
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }
}
