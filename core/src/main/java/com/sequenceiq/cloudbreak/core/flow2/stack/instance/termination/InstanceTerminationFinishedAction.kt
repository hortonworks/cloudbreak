package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

@Component("InstanceTerminationFinishedAction")
class InstanceTerminationFinishedAction : AbstractInstanceTerminationAction<RemoveInstanceResult>(RemoveInstanceResult::class.java) {
    @Inject
    private val instanceTerminationService: InstanceTerminationService? = null

    @Throws(Exception::class)
    override fun doExecute(context: InstanceTerminationContext, payload: RemoveInstanceResult, variables: Map<Any, Any>) {
        instanceTerminationService!!.finishInstanceTermination(context, payload)
        sendEvent(context)
    }

    override fun createRequest(context: InstanceTerminationContext): Selectable {
        return StackEvent(InstanceTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation(), context.stack.id)
    }
}
