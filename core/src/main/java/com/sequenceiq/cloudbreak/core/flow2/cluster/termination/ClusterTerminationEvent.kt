package com.sequenceiq.cloudbreak.core.flow2.cluster.termination

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult

enum class ClusterTerminationEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    TERMINATION_EVENT(FlowTriggers.CLUSTER_TERMINATION_TRIGGER_EVENT),
    TERMINATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterTerminationResult::class.java)),
    TERMINATION_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterTerminationResult::class.java)),

    FINALIZED_EVENT("TERMINATECLUSTERFINALIZED"),
    FAILURE_EVENT("TERMINATECLUSTERFAILUREEVENT"),
    FAIL_HANDLED_EVENT("TERMINATECLUSTERFAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
