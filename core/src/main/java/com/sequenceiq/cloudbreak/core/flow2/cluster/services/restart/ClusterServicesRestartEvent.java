package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart.ClusterServicesRestartResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterServicesRestartEvent implements FlowEvent {
    CLUSTER_SERVICES_RESTART_TRIGGER_EVENT("CLUSTER_SERVICES_RESTART_TRIGGER_EVENT"),
    CLUSTER_SERVICES_RESTART_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterServicesRestartResult.class)),
    CLUSTER_SERVICES_RESTART_FINISHED_EVENT(EventSelectorUtil.selector(ClusterServicesRestartResult.class)),

    FINALIZED_EVENT("CLUSTER_SERVICES_RESTART_FINALIZED_EVENT"),
    FAILURE_EVENT("CLUSTER_SERVICES_RESTART_FAILURE_EVENT"),
    FAIL_HANDLED_EVENT("CLUSTER_SERVICES_RESTART_FAIL_HANDLED_EVENT");

    private final String event;

    ClusterServicesRestartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
