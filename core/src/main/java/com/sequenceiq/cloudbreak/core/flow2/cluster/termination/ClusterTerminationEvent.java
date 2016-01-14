package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariClusterResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum ClusterTerminationEvent implements FlowEvent {
    TERMINATION_EVENT(FlowPhases.CLUSTER_TERMINATION.name()),
    TERMINATION_FINISHED_EVENT(AmbariClusterResult.selector(TerminateClusterResult.class)),
    TERMINATION_FAILED_EVENT(AmbariClusterResult.failureSelector(TerminateClusterResult.class)),
    TERMINATION_FINALIZED_EVENT("TERMINATECLUSTERFINALIZED"),
    CLUSTER_TERMINATION_FAIL_HANDLED_EVENT("TERMINATECLUSTERFAILHANDLED");

    private String stringRepresentation;

    ClusterTerminationEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }

    public static ClusterTerminationEvent fromString(String stringRepresentiation) {
        for (ClusterTerminationEvent event : ClusterTerminationEvent.values()) {
            if (stringRepresentiation.equalsIgnoreCase(event.stringRepresentation)) {
                return event;
            }
        }
        return null;
    }
}
