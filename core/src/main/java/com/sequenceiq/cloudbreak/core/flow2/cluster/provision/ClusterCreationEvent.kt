package com.sequenceiq.cloudbreak.core.flow2.cluster.provision

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariFailed
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess

enum class ClusterCreationEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    CLUSTER_CREATION_EVENT(FlowTriggers.CLUSTER_PROVISION_TRIGGER_EVENT),
    CLUSTER_INSTALL_EVENT(FlowTriggers.CLUSTER_INSTALL_TRIGGER_EVENT),
    START_AMBARI_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(StartAmbariServicesSuccess::class.java)),
    START_AMBARI_SERVICES_FAILED_EVENT(EventSelectorUtil.selector(StartAmbariServicesFailed::class.java)),
    START_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(StartAmbariSuccess::class.java)),
    START_AMBARI_FAILED_EVENT(EventSelectorUtil.selector(StartAmbariFailed::class.java)),
    INSTALL_CLUSTER_FINISHED_EVENT(EventSelectorUtil.selector(InstallClusterSuccess::class.java)),
    INSTALL_CLUSTER_FAILED_EVENT(EventSelectorUtil.selector(InstallClusterFailed::class.java)),
    CLUSTER_CREATION_FAILED_EVENT("CLUSTER_CREATION_FAILED"),
    CLUSTER_CREATION_FINISHED_EVENT("CLUSTER_CREATION_FINISHED"),
    CLUSTER_CREATION_FAILURE_HANDLED_EVENT("CLUSTER_CREATION_FAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
