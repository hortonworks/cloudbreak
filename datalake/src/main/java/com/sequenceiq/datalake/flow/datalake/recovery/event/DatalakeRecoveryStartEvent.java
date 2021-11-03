package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.sdx.api.model.UpgradeRecoveryType;

public class DatalakeRecoveryStartEvent extends SdxEvent {

    // TODO: RecoveryType will be used in the context of
    //  automatic data restore included in the flow
    private final UpgradeRecoveryType recoveryType;

    public DatalakeRecoveryStartEvent(String selector, Long sdxId, String userId, UpgradeRecoveryType recoveryType) {
        super(selector, sdxId, userId);
        this.recoveryType = recoveryType;
    }

    public UpgradeRecoveryType getRecoveryType() {
        return recoveryType;
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryStartEvent{" +
                "recoveryType=" + recoveryType +
                "} " + super.toString();
    }
}
