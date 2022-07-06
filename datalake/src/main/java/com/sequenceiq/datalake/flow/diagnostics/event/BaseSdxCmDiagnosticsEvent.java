package com.sequenceiq.datalake.flow.diagnostics.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class BaseSdxCmDiagnosticsEvent extends SdxEvent {

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
