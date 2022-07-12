package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeFlowChainStartEvent extends SdxEvent {

    public static final String DATALAKE_UPGRADE_FLOW_CHAIN_EVENT = "DatalakeUpgradeFlowChainEvent";

    private final String imageId;

    private final boolean replaceVms;

    private final String backupLocation;

    @JsonCreator
    public DatalakeUpgradeFlowChainStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("replaceVms") boolean replaceVms,
            @JsonProperty("backupLocation") String backupLocation) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.replaceVms = replaceVms;
        this.backupLocation = backupLocation;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isReplaceVms() {
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

    @Override
    public String toString() {
        return "DatalakeUpgradeFlowChainStartEvent{" +
                "imageId='" + imageId + '\'' +
                ", replaceVms=" + replaceVms +
                ", backupLocation='" + backupLocation + '\'' +
                "} " + super.toString();
    }
}
