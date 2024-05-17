package com.sequenceiq.environment.environment.flow.externalizedcluster.create.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalizedComputeClusterCreationStateSelectors implements FlowEvent {
    DEFAULT_COMPUTE_CLUSTER_CREATION_START_EVENT,
    DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_EVENT,
    DEFAULT_COMPUTE_CLUSTER_CREATION_FINALIZED_EVENT,
    DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_HANDLED_EVENT,
    DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
