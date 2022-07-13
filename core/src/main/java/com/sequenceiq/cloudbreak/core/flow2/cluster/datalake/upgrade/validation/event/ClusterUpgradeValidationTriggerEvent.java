package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterUpgradeValidationTriggerEvent extends StackEvent {

    private final String imageId;

    private final boolean lockComponents;

    private final boolean upgradePreparation;

    @JsonCreator
    public ClusterUpgradeValidationTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("lockComponents") boolean lockComponents,
            @JsonProperty("upgradePreparation") boolean upgradePreparation) {
        super(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT.event(), resourceId, accepted);
        this.imageId = imageId;
        this.lockComponents = lockComponents;
        this.upgradePreparation = upgradePreparation;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public boolean isUpgradePreparation() {
        return upgradePreparation;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeValidationTriggerEvent{" +
                "imageId=" + imageId +
                "lockComponents=" + lockComponents +
                "upgradePreparation=" + upgradePreparation +
                "} " + super.toString();
    }
}
