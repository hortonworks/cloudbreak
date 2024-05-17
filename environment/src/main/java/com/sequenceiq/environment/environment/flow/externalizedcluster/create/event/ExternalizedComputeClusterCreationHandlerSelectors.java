package com.sequenceiq.environment.environment.flow.externalizedcluster.create.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalizedComputeClusterCreationHandlerSelectors implements FlowEvent {

    DEFAULT_COMPUTE_CLUSTER_CREATION_WAIT_HANDLER_EVENT;

    @Override
    public String event() {
        return name();
    }
}