package com.sequenceiq.datalake.flow.diagnostics.event;

import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT;

import java.util.Map;

public class SdxDiagnosticsSuccessEvent extends BaseSdxDiagnosticsEvent {

    public SdxDiagnosticsSuccessEvent(Long sdxId, String userId, Map<String, Object> properties) {
        super(sdxId, userId, properties);
    }

    @Override
    public String selector() {
        return SDX_DIAGNOSTICS_COLLECTION_SUCCESS_EVENT.event();
    }
}
