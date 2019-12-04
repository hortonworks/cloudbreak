package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeSuccessEvent extends SdxEvent {

    public SdxUpgradeSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxUpgradeSuccessEvent";
    }
}
