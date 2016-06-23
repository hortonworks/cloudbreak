package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers

internal enum class StackTerminationEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    TERMINATION_EVENT(FlowTriggers.STACK_TERMINATE_TRIGGER_EVENT),
    FORCE_TERMINATION_EVENT(FlowTriggers.STACK_FORCE_TERMINATE_TRIGGER_EVENT),
    TERMINATION_FINISHED_EVENT(TerminateStackResult.selector(TerminateStackResult::class.java)),
    TERMINATION_FAILED_EVENT(TerminateStackResult.failureSelector(TerminateStackResult::class.java)),
    TERMINATION_FINALIZED_EVENT("TERMINATESTACKFINALIZED"),
    STACK_TERMINATION_FAIL_HANDLED_EVENT("TERMINATIONFAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
