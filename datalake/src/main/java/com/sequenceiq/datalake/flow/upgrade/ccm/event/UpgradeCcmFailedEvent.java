package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class UpgradeCcmFailedEvent extends SdxFailedEvent {

    public UpgradeCcmFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static UpgradeCcmFailedEvent from(SdxEvent event, Exception exception) {
        return new UpgradeCcmFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return UpgradeCcmFailedEvent.class.getSimpleName();
    }
}
