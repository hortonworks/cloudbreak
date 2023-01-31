package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeFlowChainStartEvent extends SdxEvent {

    public static final String DATALAKE_UPGRADE_FLOW_CHAIN_EVENT = "DatalakeUpgradeFlowChainEvent";

    private final String imageId;

    private final boolean replaceVms;

    private final String backupLocation;

    private final DatalakeDrSkipOptions skipOptions;

    private final boolean rollingUpgradeEnabled;

    private final boolean keepVariant;

    //CHECKSTYLE:OFF
    @JsonCreator
    public DatalakeUpgradeFlowChainStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("replaceVms") boolean replaceVms,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("skipOptions") DatalakeDrSkipOptions skipOptions,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("keepVariant") boolean keepVariant) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.replaceVms = replaceVms;
        this.backupLocation = backupLocation;
        this.skipOptions = skipOptions;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.keepVariant = keepVariant;
    }
    //CHECKSTYLE:ON

    public String getImageId() {
        return imageId;
    }

    public boolean isReplaceVms() {
        return replaceVms;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public DatalakeDrSkipOptions getSkipOptions() {
        return skipOptions;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public String selector() {
        return DATALAKE_UPGRADE_FLOW_CHAIN_EVENT;
    }

    public boolean isKeepVariant() {
        return keepVariant;
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
                ", rollingUpgradeEnabled='" + rollingUpgradeEnabled + '\'' +
                "} " + super.toString();
    }
}
