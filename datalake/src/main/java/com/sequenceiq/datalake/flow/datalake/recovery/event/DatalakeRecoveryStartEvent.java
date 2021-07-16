package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

public class DatalakeRecoveryStartEvent extends SdxEvent {

    public DatalakeRecoveryStartEvent(String selector, Long sdxId, String userId, SdxRecoveryType recoveryType) {
        super(selector, sdxId, userId);
        this.recoveryType = recoveryType;
    }

    private final SdxRecoveryType recoveryType;

    public SdxRecoveryType getRecoveryType() {
        return recoveryType;
    }

    @Override
    public String selector() {
        return "DatalakeRecoveryStartEvent";
    }
}
