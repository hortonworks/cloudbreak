package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxEvent;

public class BaseSdxCmDiagnosticsEvent extends SdxEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Map<String, Object> properties;

    @JsonCreator
    public BaseSdxCmDiagnosticsEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("properties") Map<String, Object> properties) {
        super(sdxId, userId);
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
