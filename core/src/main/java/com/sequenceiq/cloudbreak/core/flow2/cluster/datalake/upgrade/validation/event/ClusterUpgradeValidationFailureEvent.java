package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class ClusterUpgradeValidationFailureEvent extends ClusterUpgradeValidationEvent implements Selectable {

    private final Exception exception;

    public ClusterUpgradeValidationFailureEvent(Long resourceId, Exception exception) {
        super(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event(), resourceId, null);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event();
    }

    public Exception getException() {
        return exception;
    }
}
