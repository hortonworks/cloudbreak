package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeFlowChainStartEvent extends SdxEvent {

    private final String imageId;

    private final boolean replaceVms;

    private final String backupLocation;

    public DatalakeUpgradeFlowChainStartEvent(String selector, Long sdxId, String userId, String imageId, boolean replaceVms, String backupLocation) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.replaceVms = replaceVms;
        this.backupLocation = backupLocation;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean getReplaceVms() {
        return replaceVms;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeFlowChainEvent";
    }
}
