package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SdxDiagnosticsSuccessEvent extends BaseSdxDiagnosticsEvent {

    @JsonCreator
    public SdxDiagnosticsSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("properties") Map<String, Object> properties) {
        super(sdxId, userId, properties);
    }

    @Override
    public String selector() {
        return SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT.event();
    }
}
