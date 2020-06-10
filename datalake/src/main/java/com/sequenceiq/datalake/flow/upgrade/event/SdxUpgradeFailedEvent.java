package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxUpgradeFailedEvent extends SdxFailedEvent {

    public SdxUpgradeFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxUpgradeFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxUpgradeFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxUpgradeFailedEvent";
    }
}
