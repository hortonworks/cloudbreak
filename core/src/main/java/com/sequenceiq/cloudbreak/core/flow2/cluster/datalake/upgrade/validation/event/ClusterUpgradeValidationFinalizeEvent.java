package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeValidationFinalizeEvent extends StackEvent {

    public ClusterUpgradeValidationFinalizeEvent(Long resourceId) {
        super(ClusterUpgradeValidationStateSelectors.FINALIZE_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), resourceId);
    }
}
