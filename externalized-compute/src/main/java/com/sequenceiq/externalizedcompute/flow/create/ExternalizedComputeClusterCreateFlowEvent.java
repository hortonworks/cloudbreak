package com.sequenceiq.externalizedcompute.flow.create;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ExternalizedComputeClusterCreateFlowEvent implements FlowEvent {

    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_FINISHED_EVENT(EventSelectorUtil.selector(ExternalizedComputeClusterCreateEnvWaitSuccessResponse.class)),
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAILED_EVENT(EventSelectorUtil.selector(ExternalizedComputeClusterCreateFailedEvent.class)),
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAIL_HANDLED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_EVENT(EventSelectorUtil.selector(ExternalizedComputeClusterCreateWaitSuccessResponse.class)),
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINALIZED_EVENT;

    private final String event;

    ExternalizedComputeClusterCreateFlowEvent() {
        event = name();
    }

    ExternalizedComputeClusterCreateFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
