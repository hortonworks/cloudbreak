package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.sequenceiq.datalake.flow.SdxEvent;

public abstract class BaseSdxDiagnosticsEvent extends SdxEvent {

    private final Map<String, Object> properties;

    public BaseSdxDiagnosticsEvent(Long sdxId, String userId, Map<String, Object> properties) {
        super(sdxId, userId);
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
