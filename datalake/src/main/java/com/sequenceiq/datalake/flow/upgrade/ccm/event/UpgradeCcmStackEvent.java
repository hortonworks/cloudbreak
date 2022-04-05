package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmStackEvent extends SdxEvent {

    public UpgradeCcmStackEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(UpgradeCcmStackEvent.class, other);
    }
}
