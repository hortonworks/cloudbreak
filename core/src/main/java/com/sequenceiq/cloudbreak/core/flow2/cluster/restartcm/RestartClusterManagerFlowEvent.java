package com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum RestartClusterManagerFlowEvent implements FlowEvent {

    RESTART_CLUSTER_MANAGER_TRIGGER_EVENT,
    RESTART_CLUSTER_MANAGER_FINISHED_EVENT(EventSelectorUtil.selector(RestartClusterManagerServerSuccess.class)),
    RESTART_CLUSTER_MANAGER_FINALIZED_EVENT,
    RESTART_CLUSTER_MANAGER_FAIL_HANDLED_EVENT,
    RESTART_CLUSTER_MANAGER_FAILURE_EVENT;

    private final String event;

    RestartClusterManagerFlowEvent() {
        event = name();
    }

    RestartClusterManagerFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
