package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DnsUpdateFinished;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterStartEvent implements FlowEvent {
    CLUSTER_START_EVENT("CLUSTER_START_TRIGGER_EVENT"),
    CLUSTER_START_PILLAR_CONFIG_UPDATE_FINISHED_EVENT(EventSelectorUtil.selector(ClusterStartPillarConfigUpdateResult.class)),
    CLUSTER_START_PILLAR_CONFIG_UPDATE_FAILED_EVENT(EventSelectorUtil.selector(PillarConfigUpdateFailed.class)),
    DNS_UPDATE_FINISHED_EVENT(EventSelectorUtil.selector(DnsUpdateFinished.class)),
    CLUSTER_START_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterStartResult.class)),
    CLUSTER_START_POLLING_EVENT(EventSelectorUtil.selector(ClusterStartResult.class)),
    CLUSTER_START_POLLING_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterStartPollingResult.class)),
    CLUSTER_START_POLLING_FINISHED_EVENT(EventSelectorUtil.selector(ClusterStartPollingResult.class)),
    CONFIGURE_MANAGEMENT_SERVICES_SUCCESS_EVENT(EventSelectorUtil.selector(ConfigureClusterManagerManagementServicesSuccess.class)),
    FINALIZED_EVENT("CLUSTERSTARTFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERSTARTFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERSTARTFAILHANDLEDEVENT");

    private final String event;

    ClusterStartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
