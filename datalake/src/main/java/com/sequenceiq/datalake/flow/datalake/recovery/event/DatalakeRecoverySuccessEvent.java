package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRecoverySuccessEvent extends SdxEvent {

    public DatalakeRecoverySuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "DatalakeRecoverySuccessEvent";
    }
}
