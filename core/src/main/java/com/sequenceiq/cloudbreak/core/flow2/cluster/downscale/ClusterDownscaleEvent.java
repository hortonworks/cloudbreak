package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataResult;

public enum ClusterDownscaleEvent implements FlowEvent {
    DECOMMISSION_EVENT(FlowPhases.CLUSTER_DOWNSCALE.name()),
    DECOMMISSION_AND_DOWNSCALE_EVENT(FlowPhases.CLUSTER_AND_STACK_DOWNSCALE.name()),
    DECOMMISSION_FINISHED_EVENT(ClusterPlatformResult.selector(DecommissionResult.class)),
    DECOMMISSION_FAILED_EVENT(ClusterPlatformResult.failureSelector(DecommissionResult.class)),
    UPDATE_METADATA_FINISHED_EVENT(ClusterPlatformResult.selector(UpdateInstanceMetadataResult.class)),
    UPDATE_METADATA_FAILED_EVENT(ClusterPlatformResult.failureSelector(UpdateInstanceMetadataResult.class)),
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
