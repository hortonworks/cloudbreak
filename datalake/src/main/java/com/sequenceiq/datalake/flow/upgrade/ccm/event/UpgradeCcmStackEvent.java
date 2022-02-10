package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmStackEvent extends SdxEvent {

    private final SdxCluster sdxCluster;

    public UpgradeCcmStackEvent(String selector, SdxCluster sdxCluster, String userId) {
        super(selector, sdxCluster.getId(), userId);
        this.sdxCluster = sdxCluster;
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(UpgradeCcmStackEvent.class, other);
    }
}
