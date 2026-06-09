package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeExistingUpgradeCommandValidationEvent extends ClusterUpgradeValidationEvent {

    // TODO CB-33421: Remove image field once in-flight flow events no longer depend on it in JSON.
    private final Image image;

    @JsonCreator
    public ClusterUpgradeExistingUpgradeCommandValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties,
            @JsonProperty("image") Image image) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_EXISTING_UPGRADE_COMMAND_EVENT.name(), resourceId, imageId, clusterUpgradeProperties);
        this.image = image;
    }

    public Image getImage() {
        // TODO CB-33421: Remove image getter once in-flight flow events no longer depend on it in JSON.
        return image;
    }
}
