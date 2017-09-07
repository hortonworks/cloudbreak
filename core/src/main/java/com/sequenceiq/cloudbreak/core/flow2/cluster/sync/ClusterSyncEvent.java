package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult;

public enum ClusterSyncEvent implements FlowEvent {
    CLUSTER_SYNC_EVENT("CLUSTER_SYNC_TRIGGER_EVENT"),
    CLUSTER_SYNC_FINISHED_EVENT(EventSelectorUtil.selector(ClusterSyncResult.class)),
    CLUSTER_SYNC_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterSyncResult.class)),

    FINALIZED_EVENT("CLUSTERSYNCFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERSYNCFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERSYNCFAILHANDLEDEVENT");

    private final String event;

    ClusterSyncEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
