package com.sequenceiq.cloudbreak.core.flow2.cluster.stop

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult

enum class ClusterStopEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    CLUSTER_STOP_EVENT(FlowTriggers.CLUSTER_STOP_TRIGGER_EVENT),
    CLUSTER_STOP_FINISHED_EVENT(EventSelectorUtil.selector(ClusterStopResult::class.java)),
    CLUSTER_STOP_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterStopResult::class.java)),

    FINALIZED_EVENT("CLUSTERSTOPFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERSTOPFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERSTOPFAILHANDLEDEVENT");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
