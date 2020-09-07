package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_EVENT;

import java.util.Map;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxCmDiagnosticsFailedEvent extends SdxFailedEvent {

    private final Map<String, Object> properties;

    public SdxCmDiagnosticsFailedEvent(Long sdxId, String userId, Map<String, Object> properties, Exception exception) {
        super(sdxId, userId, exception);
        this.properties = properties;
    }

    public static SdxDiagnosticsFailedEvent from(BaseSdxCmDiagnosticsEvent event, Exception exception) {
        return new SdxDiagnosticsFailedEvent(event.getResourceId(), event.getUserId(), event.getProperties(), exception);
    }

    @Override
    public String selector() {
        return SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_EVENT.event();
    }
}
