package com.sequenceiq.cloudbreak.core.flow2.chain

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.common.type.ScalingType
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class UpscaleFlowEventChainFactory : FlowEventChainFactory<StackAndClusterUpscaleTriggerEvent> {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val hostGroupService: HostGroupService? = null

    override fun initEvent(): String {
        return FlowTriggers.FULL_UPSCALE_TRIGGER_EVENT
    }

    override fun createFlowTriggerEventQueue(event: StackAndClusterUpscaleTriggerEvent): Queue<Selectable> {
        val stack = stackService!!.getById(event.stackId)
        val cluster = stack.cluster
        val flowEventChain = ConcurrentLinkedQueue<Selectable>()
        flowEventChain.add(StackSyncTriggerEvent(FlowTriggers.STACK_SYNC_TRIGGER_EVENT, event.stackId, false))
        flowEventChain.add(StackScaleTriggerEvent(FlowTriggers.STACK_UPSCALE_TRIGGER_EVENT, event.stackId, event.instanceGroup,
                event.adjustment))
        if (ScalingType.isClusterUpScale(event.scalingType) && cluster != null) {
            val hostGroup = hostGroupService!!.getByClusterIdAndInstanceGroupName(cluster.id, event.instanceGroup)
            flowEventChain.add(ClusterScaleTriggerEvent(FlowTriggers.CLUSTER_UPSCALE_TRIGGER_EVENT, stack.id, hostGroup.name,
                    event.adjustment))
        }
        return flowEventChain
    }
}
