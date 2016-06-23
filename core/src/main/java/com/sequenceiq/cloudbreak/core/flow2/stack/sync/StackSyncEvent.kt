package com.sequenceiq.cloudbreak.core.flow2.stack.sync

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers

enum class StackSyncEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    SYNC_EVENT(FlowTriggers.STACK_SYNC_TRIGGER_EVENT),
    SYNC_FINISHED_EVENT(GetInstancesStateResult.selector(GetInstancesStateResult::class.java)),
    SYNC_FAILURE_EVENT(GetInstancesStateResult.failureSelector(GetInstancesStateResult::class.java)),
    SYNC_FINALIZED_EVENT("SYNCSTACKFINALIZED"),
    SYNC_FAIL_HANDLED_EVENT("SYNCFAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
