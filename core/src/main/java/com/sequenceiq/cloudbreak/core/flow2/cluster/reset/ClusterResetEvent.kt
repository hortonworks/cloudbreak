package com.sequenceiq.cloudbreak.core.flow2.cluster.reset

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariFailed
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult

enum class ClusterResetEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    CLUSTER_RESET_EVENT(FlowTriggers.CLUSTER_RESET_TRIGGER_EVENT),
    CLUSTER_RESET_FINISHED_EVENT(EventSelectorUtil.selector(ClusterResetResult::class.java)),
    CLUSTER_RESET_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterResetResult::class.java)),

    CLUSTER_RESET_START_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(StartAmbariSuccess::class.java)),
    CLUSTER_RESET_START_AMBARI_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(StartAmbariFailed::class.java)),

    FINALIZED_EVENT("CLUSTERRESETFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERRESETFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERRESETFAILHANDLEDEVENT");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
