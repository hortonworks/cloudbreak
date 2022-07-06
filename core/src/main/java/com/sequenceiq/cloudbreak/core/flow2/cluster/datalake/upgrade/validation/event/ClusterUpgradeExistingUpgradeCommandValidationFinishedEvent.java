package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent extends StackEvent {

    @JsonCreator
    public ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent(
            @JsonProperty("resourceId") Long resourceId) {
        super(ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT.name(), resourceId);
    }
}
