package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeStartEvent extends SdxEvent {

    private final String imageId;

    private final boolean replaceVms;

    public DatalakeUpgradeStartEvent(String selector, Long sdxId, String userId, String imageId, boolean replaceVms) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.replaceVms = replaceVms;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean getReplaceVms() {
        return replaceVms;
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeStartEvent";
    }
}
