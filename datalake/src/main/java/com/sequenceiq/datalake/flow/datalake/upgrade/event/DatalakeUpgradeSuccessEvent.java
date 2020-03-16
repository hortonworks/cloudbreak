package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeSuccessEvent extends SdxEvent {

    public DatalakeUpgradeSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeSuccessEvent";
    }
}
