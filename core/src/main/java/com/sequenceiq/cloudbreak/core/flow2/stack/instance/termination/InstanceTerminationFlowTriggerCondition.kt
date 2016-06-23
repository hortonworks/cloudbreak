package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class InstanceTerminationFlowTriggerCondition : FlowTriggerCondition {

    @Inject
    private val stackService: StackService? = null

    override fun isFlowTriggerable(stackId: Long?): Boolean {
        val stack = stackService!!.getById(stackId)
        val result = !stack.isDeleteInProgress
        if (result) {
            LOGGER.info("Couldn't start instance termination flow because the stack has been terminating.")
        }
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(InstanceTerminationFlowTriggerCondition::class.java)
    }
}
