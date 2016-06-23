package com.sequenceiq.cloudbreak.core.flow2.chain

import java.util.Queue

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable

interface FlowEventChainFactory<P : Payload> {
    fun initEvent(): String
    fun createFlowTriggerEventQueue(event: P): Queue<Selectable>
}
