package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeStartEvent extends SdxEvent {

    private String imageId;

    public DatalakeUpgradeStartEvent(String selector, Long sdxId, String userId, String imageId) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeStartEvent";
    }
}
