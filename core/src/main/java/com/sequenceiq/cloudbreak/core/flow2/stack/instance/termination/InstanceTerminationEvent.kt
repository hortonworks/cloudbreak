package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers

internal enum class InstanceTerminationEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    TERMINATION_EVENT(FlowTriggers.REMOVE_INSTANCE_TRIGGER_EVENT),
    TERMINATION_FINISHED_EVENT(RemoveInstanceResult.selector(RemoveInstanceResult::class.java)),
    TERMINATION_FAILED_EVENT(RemoveInstanceResult.failureSelector(RemoveInstanceResult::class.java)),
    TERMINATION_FINALIZED_EVENT("TERMINATEINSTANCEFINALIZED"),
    TERMINATION_FAIL_HANDLED_EVENT("TERMINATIONINSTANCEFAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
