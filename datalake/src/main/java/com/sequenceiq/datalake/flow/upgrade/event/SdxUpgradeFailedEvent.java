package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxUpgradeFailedEvent extends SdxFailedEvent {

    public SdxUpgradeFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    @Override
    public String selector() {
        return "SdxUpgradeFailedEvent";
    }
}
