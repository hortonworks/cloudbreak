package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_EVENT;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxCmDiagnosticsFailedEvent extends SdxFailedEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Map<String, Object> properties;

    @JsonCreator
    public SdxCmDiagnosticsFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
        this.properties = properties;
    }

    public static SdxDiagnosticsFailedEvent from(BaseSdxCmDiagnosticsEvent event, Exception exception) {
        return new SdxDiagnosticsFailedEvent(event.getResourceId(), event.getUserId(), event.getProperties(), exception);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String selector() {
        return SDX_CM_DIAGNOSTICS_COLLECTION_FAILED_EVENT.event();
    }
}
