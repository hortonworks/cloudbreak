package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradePreparationFlowChainStartEvent extends SdxEvent {

    public static final String DATALAKE_UPGRADE_PREPARATION_FLOW_CHAIN_EVENT = "DatalakeUpgradePreparationFlowChainEvent";

    private final String imageId;

    private final String backupLocation;

    @JsonCreator
    public DatalakeUpgradePreparationFlowChainStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("backupLocation") String backupLocation) {
        super(sdxId, userId);
        this.imageId = imageId;
        this.backupLocation = backupLocation;
    }

    public String getImageId() {
        return imageId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    @Override
    public String selector() {
        return DATALAKE_UPGRADE_PREPARATION_FLOW_CHAIN_EVENT;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeUpgradePreparationFlowChainStartEvent.class, other,
                event -> Objects.equals(imageId, event.imageId) && Objects.equals(backupLocation, event.backupLocation));
    }

    @Override
    public String toString() {
        return "DatalakeUpgradePreparationStartEvent{" +
                "imageId='" + imageId + '\'' +
                "backupLocation='" + backupLocation + '\'' +
                "} " + super.toString();
    }
}
