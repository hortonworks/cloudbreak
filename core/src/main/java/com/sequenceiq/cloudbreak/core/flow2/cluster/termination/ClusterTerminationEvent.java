package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterTerminationResult;

public enum ClusterTerminationEvent implements FlowEvent {
    TERMINATION_EVENT(FlowTriggers.CLUSTER_TERMINATION_TRIGGER_EVENT),
    TERMINATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterTerminationResult.class)),
    TERMINATION_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterTerminationResult.class)),

    FINALIZED_EVENT("TERMINATECLUSTERFINALIZED"),
    FAILURE_EVENT("TERMINATECLUSTERFAILUREEVENT"),
    FAIL_HANDLED_EVENT("TERMINATECLUSTERFAILHANDLED");

    private String stringRepresentation;

    ClusterTerminationEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
