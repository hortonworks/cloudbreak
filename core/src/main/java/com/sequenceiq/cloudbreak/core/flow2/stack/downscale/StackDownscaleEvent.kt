package com.sequenceiq.cloudbreak.core.flow2.stack.downscale

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers

enum class StackDownscaleEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    DOWNSCALE_EVENT(FlowTriggers.STACK_DOWNSCALE_TRIGGER_EVENT),
    DOWNSCALE_FINISHED_EVENT(DownscaleStackResult.selector(DownscaleStackResult::class.java)),
    DOWNSCALE_FAILURE_EVENT(DownscaleStackResult.failureSelector(DownscaleStackResult::class.java)),
    DOWNSCALE_FINALIZED_EVENT("DOWNSCALESTACKFINALIZED"),
    DOWNSCALE_FAIL_HANDLED_EVENT("DOWNSCALEFAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
