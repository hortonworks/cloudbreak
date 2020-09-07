package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxCmDiagnosticsWaitRequest extends BaseSdxCmDiagnosticsEvent {

    private final FlowIdentifier flowIdentifier;

    public SdxCmDiagnosticsWaitRequest(Long sdxId, String userId, Map<String, Object> properties, FlowIdentifier flowIdentifier) {
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
