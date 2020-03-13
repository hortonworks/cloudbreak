package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DnsUpdateFinished;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;

public enum ClusterStartEvent implements FlowEvent {
    CLUSTER_START_EVENT("CLUSTER_START_TRIGGER_EVENT"),
    DNS_UPDATE_FINISHED_EVENT(EventSelectorUtil.selector(DnsUpdateFinished.class)),
    CLUSTER_START_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterStartResult.class)),
    CLUSTER_START_POLLING_EVENT(EventSelectorUtil.selector(ClusterStartResult.class)),
    CLUSTER_START_POLLING_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterStartPollingResult.class)),
    CLUSTER_START_FINISHED_EVENT(EventSelectorUtil.selector(ClusterStartPollingResult.class)),

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
