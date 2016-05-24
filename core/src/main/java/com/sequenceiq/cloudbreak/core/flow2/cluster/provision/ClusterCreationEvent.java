package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess;

public enum ClusterCreationEvent implements FlowEvent {
    CLUSTER_CREATION_EVENT(FlowPhases.RUN_CLUSTER_CONTAINERS.name()),
    START_AMBARI_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(StartAmbariServicesSuccess.class)),
    START_AMBARI_SERVICES_FAILED_EVENT(EventSelectorUtil.selector(StartAmbariServicesFailed.class)),
    START_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(StartAmbariSuccess.class)),
    START_AMBARI_FAILED_EVENT(EventSelectorUtil.selector(StartAmbariFailed.class)),
    INSTALL_CLUSTER_FINISHED_EVENT(EventSelectorUtil.selector(InstallClusterSuccess.class)),
    INSTALL_CLUSTER_FAILED_EVENT(EventSelectorUtil.selector(InstallClusterFailed.class)),
    CLUSTER_CREATION_FAILED_EVENT("CLUSTER_CREATION_FAILED"),
    CLUSTER_CREATION_FINISHED_EVENT("CLUSTER_CREATION_FINISHED"),
    CLUSTER_CREATION_FAILURE_HANDLED_EVENT("CLUSTER_CREATION_FAILHANDLED");

    private String stringRepresentation;

    ClusterCreationEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
