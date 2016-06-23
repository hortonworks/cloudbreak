package com.sequenceiq.cloudbreak.core.flow2.stack.stop

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class StackStopFlowTriggerCondition : FlowTriggerCondition {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val stackStartStopService: StackStartStopService? = null

    override fun isFlowTriggerable(stackId: Long?): Boolean {
        val stack = stackService!!.getById(stackId)
        return stackStartStopService!!.isStopPossible(stack)
    }
}
