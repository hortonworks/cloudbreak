package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterServicesRestartPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterServicesRestartResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterServicesRestartEvent implements FlowEvent {
    CLUSTER_SERVICES_RESTART_TRIGGER_EVENT("CLUSTER_SERVICES_RESTART_TRIGGER_EVENT"),
    CLUSTER_SERVICES_RESTART_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterServicesRestartResult.class)),
    CLUSTER_SERVICES_RESTART_POLLING_EVENT(EventSelectorUtil.selector(ClusterServicesRestartResult.class)),
    CLUSTER_SERVICES_RESTART_POLLING_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterServicesRestartPollingResult.class)),
    CLUSTER_SERVICES_RESTART_POLLING_FINISHED_EVENT(EventSelectorUtil.selector(ClusterServicesRestartPollingResult.class)),

    FINALIZED_EVENT("CLUSTERSERVICESRESTARTFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERSERVICESRESTARTFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERSERVICESRESTARTFAILHANDLEDEVENT");

    private final String event;

    ClusterServicesRestartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
