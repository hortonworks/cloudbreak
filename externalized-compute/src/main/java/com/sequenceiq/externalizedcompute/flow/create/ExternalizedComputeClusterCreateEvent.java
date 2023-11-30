package com.sequenceiq.externalizedcompute.flow.create;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ExternalizedComputeClusterCreateEvent implements FlowEvent {

    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_STARTED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAILED_EVENT(EventSelectorUtil.selector(ExternalizedComputeClusterCreateFailedEvent.class)),
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAIL_HANDLED_EVENT,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_EVENT(EventSelectorUtil.selector(ExternalizedComputeClusterCreateWaitSuccessResponse.class)),
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINALIZED_EVENT;

    private final String event;

    ExternalizedComputeClusterCreateEvent() {
        event = name();
    }

    ExternalizedComputeClusterCreateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
