package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult;

public enum ClusterResetEvent implements FlowEvent {
    CLUSTER_RESET_EVENT("CLUSTER_RESET_TRIGGER_EVENT"),
    CLUSTER_RESET_FINISHED_EVENT(EventSelectorUtil.selector(ClusterResetResult.class)),
    CLUSTER_RESET_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterResetResult.class)),

    CLUSTER_RESET_START_CLUSTER_MANAGER_FINISHED_EVENT(EventSelectorUtil.selector(StartClusterSuccess.class)),
    CLUSTER_RESET_START_CLUSTER_MANAGER_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(StartClusterFailed.class)),

    FINALIZED_EVENT("CLUSTERRESETFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERRESETFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERRESETFAILHANDLEDEVENT");

    private final String event;

    ClusterResetEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
