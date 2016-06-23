package com.sequenceiq.cloudbreak.core.flow2.cluster.start

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult

enum class ClusterStartEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    CLUSTER_START_EVENT(FlowTriggers.CLUSTER_START_TRIGGER_EVENT),
    CLUSTER_START_FINISHED_EVENT(EventSelectorUtil.selector(ClusterStartResult::class.java)),
    CLUSTER_START_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterStartResult::class.java)),

    FINALIZED_EVENT("CLUSTERSTARTFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERSTARTFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERSTARTFAILHANDLEDEVENT");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
