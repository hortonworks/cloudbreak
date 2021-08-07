package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

public class DatalakeRecoveryStartEvent extends SdxEvent {

    // TODO: RecoveryType will be used in the context of
    //  automatic data restore included in the flow
    private final SdxRecoveryType recoveryType;

    public DatalakeRecoveryStartEvent(String selector, Long sdxId, String userId, SdxRecoveryType recoveryType) {
        super(selector, sdxId, userId);
        this.recoveryType = recoveryType;
    }

    public SdxRecoveryType getRecoveryType() {
        return recoveryType;
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryStartEvent{" +
                "recoveryType=" + recoveryType +
                "} " + super.toString();
    }
}
