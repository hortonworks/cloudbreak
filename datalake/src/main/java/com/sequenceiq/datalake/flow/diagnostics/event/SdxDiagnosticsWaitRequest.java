package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxDiagnosticsWaitRequest extends BaseSdxDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    public SdxDiagnosticsWaitRequest(Long sdxId, String userId, Map<String, Object> properties, FlowIdentifier flowIdentifier) {
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
