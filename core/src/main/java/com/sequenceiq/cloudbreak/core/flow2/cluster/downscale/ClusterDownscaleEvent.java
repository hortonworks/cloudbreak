package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;

public enum ClusterDownscaleEvent implements FlowEvent {
    DECOMMISSION_EVENT("CLUSTER_DOWNSCALE_TRIGGER_EVENT"),
    COLLECT_CANDIDATES_FINISHED_EVENT(EventSelectorUtil.selector(CollectDownscaleCandidatesResult.class)),
    COLLECT_CANDIDATES_FAILED_EVENT(EventSelectorUtil.failureSelector(CollectDownscaleCandidatesResult.class)),
    DECOMMISSION_FINISHED_EVENT(EventSelectorUtil.selector(DecommissionResult.class)),
    DECOMMISSION_FAILED_EVENT(EventSelectorUtil.failureSelector(DecommissionResult.class)),
    REMOVE_HOSTS_FROM_ORCHESTRATOR_FINISHED(EventSelectorUtil.selector(RemoveHostsSuccess.class)),
    REMOVE_HOSTS_FROM_ORCHESTRATOR_FAILED(EventSelectorUtil.selector(RemoveHostsFailed.class)),
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
