package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_FAILED_EVENT;

import java.util.Map;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDiagnosticsFailedEvent extends SdxFailedEvent {

    private final Map<String, Object> properties;

    public SdxDiagnosticsFailedEvent(Long sdxId, String userId, Map<String, Object> properties, Exception exception) {
        super(sdxId, userId, exception);
        this.properties = properties;
    }

    public static SdxDiagnosticsFailedEvent from(BaseSdxDiagnosticsEvent event, Exception exception) {
        return new SdxDiagnosticsFailedEvent(event.getResourceId(), event.getUserId(), event.getProperties(), exception);
    }

    @Override
    public String selector() {
        return SDX_DIAGNOSTICS_COLLECTION_FAILED_EVENT.event();
    }
}
