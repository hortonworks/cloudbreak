package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;

public enum ClusterTerminationEvent implements FlowEvent {
    TERMINATION_EVENT("CLUSTER_TERMINATION_TRIGGER_EVENT"),
    TERMINATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterTerminationResult.class)),
    TERMINATION_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterTerminationResult.class)),

    FINALIZED_EVENT("TERMINATECLUSTERFINALIZED"),
    FAILURE_EVENT("TERMINATECLUSTERFAILUREEVENT"),
    FAIL_HANDLED_EVENT("TERMINATECLUSTERFAILHANDLED");

    private final String event;

    ClusterTerminationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
