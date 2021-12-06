package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent extends StackEvent {

    public ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent(Long resourceId) {
        super(ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT.name(), resourceId);
    }
}
