package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

public class SdxDiagnosticsCollectionEvent extends BaseSdxDiagnosticsEvent {
    public SdxDiagnosticsCollectionEvent(Long sdxId, String userId, Map<String, Object> properties) {
        super(sdxId, userId, properties);
    }
}
