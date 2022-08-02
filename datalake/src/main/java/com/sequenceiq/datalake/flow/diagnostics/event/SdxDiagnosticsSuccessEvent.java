package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class SdxDiagnosticsSuccessEvent extends BaseSdxDiagnosticsEvent {

    @JsonCreator
    public SdxDiagnosticsSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonTypeInfo(use = CLASS, property = "@type") @JsonProperty("properties") Map<String, Object> properties) {
        super(sdxId, userId, properties);
    }

    @Override
    public String selector() {
        return SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT.event();
    }
}
