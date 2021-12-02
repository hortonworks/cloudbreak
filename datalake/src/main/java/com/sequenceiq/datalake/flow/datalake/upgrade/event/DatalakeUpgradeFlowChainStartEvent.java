package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeFlowChainStartEvent extends SdxEvent {

    public static final String DATALAKE_UPGRADE_FLOW_CHAIN_EVENT = "DatalakeUpgradeFlowChainEvent";

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
        return DATALAKE_UPGRADE_FLOW_CHAIN_EVENT;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeUpgradeFlowChainStartEvent.class, other,
                event -> Objects.equals(imageId, event.imageId)
                        && replaceVms == event.replaceVms
                        && Objects.equals(backupLocation, event.backupLocation));
    }
}
