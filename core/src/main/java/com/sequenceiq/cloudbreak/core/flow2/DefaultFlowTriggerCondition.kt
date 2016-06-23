package com.sequenceiq.cloudbreak.core.flow2

import org.springframework.stereotype.Component

@Component
class DefaultFlowTriggerCondition : FlowTriggerCondition {
    override fun isFlowTriggerable(stackId: Long?): Boolean {
        return true
    }
}
