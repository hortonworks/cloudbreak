package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.PREPARE_PARCEL_SETTINGS_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class ClusterUpgradeParcelSettingsPreparationEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    @JsonCreator
    public ClusterUpgradeParcelSettingsPreparationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto) {
        super(PREPARE_PARCEL_SETTINGS_EVENT.name(), resourceId);
        this.imageChangeDto = imageChangeDto;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeParcelSettingsPreparationEvent{" +
                "imageChangeDto=" + imageChangeDto +
                "} " + super.toString();
    }
}
