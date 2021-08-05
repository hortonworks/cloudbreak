package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeDiskSpaceValidationFinishedEvent extends StackEvent {

    public ClusterUpgradeDiskSpaceValidationFinishedEvent(Long resourceId) {
        super(ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.name(), resourceId);
    }
}
