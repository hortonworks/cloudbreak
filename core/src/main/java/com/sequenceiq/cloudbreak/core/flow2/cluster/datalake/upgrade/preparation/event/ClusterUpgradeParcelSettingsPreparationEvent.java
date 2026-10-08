package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.PREPARE_PARCEL_SETTINGS_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.common.model.OsType;

public class ClusterUpgradeParcelSettingsPreparationEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    private final OsType currentOsType;

    private final ClusterUpgradeProperties clusterUpgradeProperties;

    @JsonCreator
    public ClusterUpgradeParcelSettingsPreparationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto,
            @JsonProperty("currentOsType") OsType currentOsType,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(PREPARE_PARCEL_SETTINGS_EVENT.name(), resourceId);
        this.imageChangeDto = imageChangeDto;
        this.currentOsType = currentOsType;
        this.clusterUpgradeProperties = clusterUpgradeProperties;
    }

    public ClusterUpgradeProperties getClusterUpgradeProperties() {
        return clusterUpgradeProperties;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    @JsonProperty("currentOsType")
    private OsType getCurrentOsType() {
        return currentOsType;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeParcelSettingsPreparationEvent{" +
                "imageChangeDto=" + imageChangeDto +
                "currentOsType=" + currentOsType +
                "clusterUpgradeProperties=" + clusterUpgradeProperties +
                "} " + super.toString();
    }
}
