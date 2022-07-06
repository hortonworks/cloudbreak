package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeExistingUpgradeCommandValidationEvent extends StackEvent {

    private final Image image;

    @JsonCreator
    public ClusterUpgradeExistingUpgradeCommandValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("image") Image image) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_EXISTING_UPGRADE_COMMAND_EVENT.name(), resourceId);
        this.image = image;
    }

    public Image getImage() {
        return image;
    }
}
