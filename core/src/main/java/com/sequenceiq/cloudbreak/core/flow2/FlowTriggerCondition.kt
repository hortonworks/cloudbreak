package com.sequenceiq.cloudbreak.core.flow2

interface FlowTriggerCondition {
    fun isFlowTriggerable(stackId: Long?): Boolean
}
