package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class ClusterUpgradePreparationTriggerEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    @JsonCreator
    public ClusterUpgradePreparationTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto) {
        super(START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT.event(), resourceId, accepted);
        this.imageChangeDto = imageChangeDto;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    @Override
    public String toString() {
        return "ClusterUpgradePreparationTriggerEvent{" +
                "selector='" + selector() + '\'' +
                ", imageChangeDto='" + imageChangeDto + '\'' +
                '}' + super.toString();
    }
}
