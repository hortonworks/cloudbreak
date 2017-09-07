package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult;

public enum ClusterStopEvent implements FlowEvent {
    CLUSTER_STOP_EVENT("CLUSTER_STOP_TRIGGER_EVENT"),
    CLUSTER_STOP_FINISHED_EVENT(EventSelectorUtil.selector(ClusterStopResult.class)),
    CLUSTER_STOP_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterStopResult.class)),

    FINALIZED_EVENT("CLUSTERSTOPFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERSTOPFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERSTOPFAILHANDLEDEVENT");

    private final String event;

    ClusterStopEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
