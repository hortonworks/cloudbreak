package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmSuccessEvent extends SdxEvent {

    public UpgradeCcmSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return UpgradeCcmSuccessEvent.class.getSimpleName();
    }
}
