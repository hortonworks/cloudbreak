package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeValidationFailedEvent extends SdxEvent {

    public DatalakeUpgradeValidationFailedEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeValidationFailedEvent";
    }

}
