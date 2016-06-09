package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataResult;

public enum ClusterDownscaleEvent implements FlowEvent {
    DECOMMISSION_EVENT(FlowTriggers.CLUSTER_DOWNSCALE_TRIGGER_EVENT),
    DECOMMISSION_FINISHED_EVENT(EventSelectorUtil.selector(DecommissionResult.class)),
    DECOMMISSION_FAILED_EVENT(EventSelectorUtil.failureSelector(DecommissionResult.class)),
    UPDATE_METADATA_FINISHED_EVENT(EventSelectorUtil.selector(UpdateInstanceMetadataResult.class)),
    UPDATE_METADATA_FAILED_EVENT(EventSelectorUtil.failureSelector(UpdateInstanceMetadataResult.class)),
    FINALIZED_EVENT("CLUSTERDOWNSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERDOWNSCALEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERDOWNSCALEFAILHANDLEDEVENT");

    private String stringRepresentation;

    ClusterDownscaleEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
