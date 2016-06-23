package com.sequenceiq.cloudbreak.core.flow2.stack.start

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers

enum class StackStartEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    START_EVENT(FlowTriggers.STACK_START_TRIGGER_EVENT),
    START_FINISHED_EVENT(StartInstancesResult.selector(StartInstancesResult::class.java)),
    START_FAILURE_EVENT(StartInstancesResult.failureSelector(StartInstancesResult::class.java)),
    COLLECT_METADATA_FINISHED_EVENT(CollectMetadataResult.selector(CollectMetadataResult::class.java)),
    COLLECT_METADATA_FAILED_EVENT(CollectMetadataResult.failureSelector(CollectMetadataResult::class.java)),
    START_FINALIZED_EVENT("STARTSTACKFINALIZED"),
    START_FAIL_HANDLED_EVENT("STARTFAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }

}
