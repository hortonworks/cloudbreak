package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxEvent;

public abstract class BaseSdxDiagnosticsEvent extends SdxEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Map<String, Object> properties;

    public BaseSdxDiagnosticsEvent(Long sdxId, String userId, Map<String, Object> properties) {
        super(sdxId, userId);
        this.properties = properties;
    }

    @JsonTypeInfo(use = CLASS, property = "@type")
    public Map<String, Object> getProperties() {
        return properties;
    }
}
