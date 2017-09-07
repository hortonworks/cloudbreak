package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateSuccess;

public enum EphemeralClusterEvent implements FlowEvent {
    EPHEMERAL_CLUSTER_UPDATE_TRIGGER_EVENT("EPHEMERAL_CLUSTER_UPDATE_TRIGGER_EVENT"),
    EPHEMERAL_CLUSTER_UPDATE_FINISHED(EventSelectorUtil.selector(EphemeralClusterUpdateSuccess.class)),
    EPHEMERAL_CLUSTER_UPDATE_FAILED(EventSelectorUtil.selector(EphemeralClusterUpdateFailed.class)),
    EPHEMERAL_CLUSTER_FLOW_FINISHED("EPHEMERAL_CLUSTER_FLOW_FINISHED"),
    EPHEMERAL_CLUSTER_FAILURE_HANDLED("EPHEMERAL_CLUSTER_FAILURE_HANDLED");

    private final String event;

    EphemeralClusterEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
