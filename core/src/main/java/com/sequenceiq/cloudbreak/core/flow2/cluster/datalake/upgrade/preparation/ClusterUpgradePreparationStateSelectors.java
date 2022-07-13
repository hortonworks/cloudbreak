package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation;

import com.sequenceiq.flow.core.FlowEvent;

public enum ClusterUpgradePreparationStateSelectors implements FlowEvent {

    START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT,
    START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT,
    START_CLUSTER_UPGRADE_PARCEL_DISTRIBUTION_EVENT,
    FINISH_CLUSTER_UPGRADE_PREPARATION_EVENT,
    FINALIZE_CLUSTER_UPGRADE_PREPARATION_EVENT,
    FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT,
    HANDLED_FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;

    @Override
    public String event() {
        return name();
    }
}
