package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeValidationFinishedEvent extends StackEvent {

    private Exception exception;

    public ClusterUpgradeValidationFinishedEvent(Long resourceId) {
        super(FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), resourceId);
    }

    public ClusterUpgradeValidationFinishedEvent(Long resourceId, Exception exception) {
        super(FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), resourceId);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }
}
