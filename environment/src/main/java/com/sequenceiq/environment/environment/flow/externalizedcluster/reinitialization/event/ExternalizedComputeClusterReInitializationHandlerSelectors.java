package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalizedComputeClusterReInitializationHandlerSelectors implements FlowEvent {

    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_WAIT_HANDLER_EVENT;

    @Override
    public String event() {
        return name();
    }
}