package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

enum ClusterUpscaleEvent implements FlowEvent {

    CLUSTER_UPSCALE_ADD_CONTAINERS_EVENT(FlowPhases.ADD_CLUSTER_CONTAINERS.name()),
    CLUSTER_UPSCALE_ADD_CONTAINERS_FINISHED_EVENT,
    CLUSTER_UPSCALE_INSTALL_AMBARI_NODES_FINISHED_EVENT,
    CLUSTER_UPSCALE_SSSD_CONFIG_FINISHED_EVENT,
    CLUSTER_UPSCALE_INSTALL_RECIPES_FINISHED_EVENT,
    CLUSTER_UPSCALE_EXECUTE_PRE_RECIPES_FINISHED_EVENT,
    CLUSTER_UPSCALE_INSTALl_SERVICES_FINISHED_EVENT,
    CLUSTER_UPSCALE_EXECUTE_POST_RECIPES_FINISHED_EVENT,
    CLUSTER_UPSCALE_FAILURE_EVENT,
    CLUSTER_UPSCALE_FINALIZED_EVENT,
    CLUSTER_UPSCALE_FAIL_HANDLED_EVENT;

    private String stringRepresentation;

    ClusterUpscaleEvent() {
        this.stringRepresentation = name();
    }

    ClusterUpscaleEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
