package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;

public enum ClusterDownscaleEvent implements FlowEvent {
    DECOMMISSION_EVENT("CLUSTER_DOWNSCALE_TRIGGER_EVENT"),
    COLLECT_CANDIDATES_FINISHED_EVENT(EventSelectorUtil.selector(CollectDownscaleCandidatesResult.class)),
    COLLECT_CANDIDATES_FAILED_EVENT(EventSelectorUtil.failureSelector(CollectDownscaleCandidatesResult.class)),
    DECOMMISSION_FINISHED_EVENT(EventSelectorUtil.selector(DecommissionResult.class)),
    DECOMMISSION_FAILED_EVENT(EventSelectorUtil.failureSelector(DecommissionResult.class)),
    FINALIZED_EVENT("CLUSTERDOWNSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERDOWNSCALEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERDOWNSCALEFAILHANDLEDEVENT");

    private final String event;

    ClusterDownscaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
