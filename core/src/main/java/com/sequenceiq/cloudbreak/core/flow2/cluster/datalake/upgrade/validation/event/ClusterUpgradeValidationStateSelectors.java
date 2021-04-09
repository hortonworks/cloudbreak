package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ClusterUpgradeValidationStateSelectors implements FlowEvent {

    START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT,
    START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT,
    FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT,
    FINALIZE_CLUSTER_UPGRADE_VALIDATION_EVENT,
    FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT,
    HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;

    @Override
    public String event() {
        return name();
    }
}
