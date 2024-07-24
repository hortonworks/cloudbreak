package com.sequenceiq.externalizedcompute.flow.delete;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ExternalizedComputeClusterDeleteFlowEvent implements FlowEvent {

    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_INITIATED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_STARTED,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_EVENT(EventSelectorUtil.selector(ExternalizedComputeClusterDeleteFailedEvent.class)),
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_EVENT(EventSelectorUtil.selector(ExternalizedComputeClusterDeleteWaitSuccessResponse.class)),
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT;

    private final String event;

    ExternalizedComputeClusterDeleteFlowEvent() {
        event = name();
    }

    ExternalizedComputeClusterDeleteFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
