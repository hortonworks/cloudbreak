package com.sequenceiq.cloudbreak.core.flow2.config

import com.sequenceiq.cloudbreak.core.flow2.Flow
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition

interface FlowConfiguration<E : FlowEvent> {
    fun createFlow(flowId: String): Flow
    val flowTriggerCondition: FlowTriggerCondition

    val events: Array<E>
    val initEvents: Array<E>
}
