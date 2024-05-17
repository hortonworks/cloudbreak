package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalizedComputeClusterReInitializationStateSelectors implements FlowEvent {
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_EVENT,
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_EVENT,
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINALIZED_EVENT,
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_HANDLED_EVENT,
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
