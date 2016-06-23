package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest
import com.sequenceiq.cloudbreak.core.flow2.event.InstanceTerminationTriggerEvent

@Component("InstanceTerminationAction")
class InstanceTerminationAction : AbstractInstanceTerminationAction<InstanceTerminationTriggerEvent>(InstanceTerminationTriggerEvent::class.java) {
    @Inject
    private val instanceTerminationService: InstanceTerminationService? = null

    @Throws(Exception::class)
    override fun doExecute(context: InstanceTerminationContext, payload: InstanceTerminationTriggerEvent, variables: Map<Any, Any>) {
        instanceTerminationService!!.instanceTermination(context)
        sendEvent(context)
    }

    override fun createRequest(context: InstanceTerminationContext): Selectable {
        return RemoveInstanceRequest<Any>(context.cloudContext, context.cloudCredential, context.cloudStack,
                context.cloudResources, context.cloudInstance)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(InstanceTerminationAction::class.java)
    }
}
