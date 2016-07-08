package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess;

public enum ClusterCreationEvent implements FlowEvent {
    CLUSTER_CREATION_EVENT(FlowTriggers.CLUSTER_PROVISION_TRIGGER_EVENT),
    CLUSTER_INSTALL_EVENT(FlowTriggers.CLUSTER_INSTALL_TRIGGER_EVENT),
    BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess.class)),
    BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed.class)),
    HOST_METADATASETUP_FINISHED_EVENT(EventSelectorUtil.selector(HostMetadataSetupSuccess.class)),
    HOST_METADATASETUP_FAILED_EVENT(EventSelectorUtil.selector(HostMetadataSetupFailed.class)),
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
