package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeExistingUpgradeCommandValidationEvent extends StackEvent {

    private final Image image;

    public ClusterUpgradeExistingUpgradeCommandValidationEvent(Long resourceId, Image image) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_EXISTING_UPGRADE_COMMAND_EVENT.name(), resourceId);
        this.image = image;
    }

    public Image getImage() {
        return image;
    }
}
