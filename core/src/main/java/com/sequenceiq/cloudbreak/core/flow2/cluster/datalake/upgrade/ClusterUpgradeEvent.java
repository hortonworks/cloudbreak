package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterUpgradeEvent implements FlowEvent {
    CLUSTER_UPGRADE_INIT_EVENT("CLUSTER_UPGRADE_INIT_EVENT"),
    CLUSTER_UPGRADE_INIT_FINISHED_EVENT(EventSelectorUtil.selector(ClusterUpgradeInitSuccess.class)),

    CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT("CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT"),

    CLUSTER_UPGRADE_CONFIGURE_MANAGEMENT_SERVICES_SUCCESS_EVENT(EventSelectorUtil.selector(ConfigureClusterManagerManagementServicesSuccess.class)),
    CLUSTER_UPGRADE_CONFIGURE_MANAGEMENT_SERVICES_FAILED_EVENT(EventSelectorUtil.selector(ConfigureClusterManagerManagementServicesFailed.class)),

    CLUSTER_UPGRADE_FINISHED_EVENT("CLUSTER_UPGRADE_FINISHED_EVENT"),

    CLUSTER_UPGRADE_FAILED_EVENT("CLUSTER_UPGRADE_FAILED_EVENT"),
    CLUSTER_UPGRADE_FAIL_HANDLED_EVENT("CLUSTER_UPGRADE_FAIL_HANDLED_EVENT"),
    CLUSTER_UPGRADE_FINALIZED_EVENT("CLUSTER_UPGRADE_FINALIZED_EVENT");

    private final String event;

    ClusterUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
