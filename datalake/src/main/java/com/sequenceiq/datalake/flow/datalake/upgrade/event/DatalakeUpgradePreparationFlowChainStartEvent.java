package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradePreparationFlowChainStartEvent extends SdxEvent {

    public static final String DATALAKE_UPGRADE_PREPARATION_FLOW_CHAIN_EVENT = "DatalakeUpgradePreparationFlowChainEvent";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ClouderaManagerV4Request components;

    private final String imageId;

    private final String backupLocation;

    @JsonCreator
    public DatalakeUpgradePreparationFlowChainStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("components") ClouderaManagerV4Request components) {
        super(DATALAKE_UPGRADE_PREPARATION_FLOW_CHAIN_EVENT, sdxId, userId);
        this.imageId = imageId;
        this.backupLocation = backupLocation;
        this.components = components;
    }

    public ClouderaManagerV4Request getComponents() {
        return components;
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
                event -> Objects.equals(imageId, event.imageId) && Objects.equals(backupLocation, event.backupLocation)
                        && Objects.equals(components, event.components));
    }

    @Override
    public String toString() {
        return "DatalakeUpgradePreparationFlowChainStartEvent{" +
                "imageId='" + imageId + '\'' +
                ", backupLocation='" + backupLocation + '\'' +
                ", components=" + components +
                "} " + super.toString();
    }
}
