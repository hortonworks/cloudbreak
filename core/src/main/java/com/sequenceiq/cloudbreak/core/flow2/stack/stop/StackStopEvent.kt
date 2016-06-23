package com.sequenceiq.cloudbreak.core.flow2.stack.stop

import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers

enum class StackStopEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    STOP_EVENT(FlowTriggers.STACK_STOP_TRIGGER_EVENT),
    STOP_FINISHED_EVENT(StopInstancesResult.selector(StopInstancesResult::class.java)),
    STOP_FAILURE_EVENT(StopInstancesResult.failureSelector(StopInstancesResult::class.java)),
    STOP_FINALIZED_EVENT("STOPSTACKFINALIZED"),
    STOP_FAIL_HANDLED_EVENT("STOPFAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }

}
