package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.sequenceiq.datalake.flow.SdxContext;

public class SdxDiagnosticsWaitRequest extends BaseSdxDiagnosticsEvent {

    public SdxDiagnosticsWaitRequest(Long sdxId, String userId, Map<String, Object> properties) {
        super(sdxId, userId, properties);
    }

    public static SdxDiagnosticsWaitRequest from(SdxContext context,
            SdxDiagnosticsCollectionEvent event) {
        return new SdxDiagnosticsWaitRequest(context.getSdxId(), context.getUserId(), event.getProperties());
    }

    @Override
    public String selector() {
        return "SdxDiagnosticsWaitRequest";
    }

}
