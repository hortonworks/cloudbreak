package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SdxCmDiagnosticsSuccessEvent extends BaseSdxCmDiagnosticsEvent {

    @JsonCreator
    public SdxCmDiagnosticsSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("properties") Map<String, Object> properties) {
        super(sdxId, userId, properties);
    }

    @Override
    public String selector() {
        return SDX_CM_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT.event();
    }
}
