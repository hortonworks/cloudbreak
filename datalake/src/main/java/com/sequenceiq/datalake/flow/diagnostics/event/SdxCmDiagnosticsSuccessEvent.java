package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT;

import java.util.Map;

public class SdxCmDiagnosticsSuccessEvent extends BaseSdxCmDiagnosticsEvent {

    public SdxCmDiagnosticsSuccessEvent(Long sdxId, String userId, Map<String, Object> properties) {
        super(sdxId, userId, properties);
    }

    @Override
    public String selector() {
        return SDX_CM_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT.event();
    }
}
