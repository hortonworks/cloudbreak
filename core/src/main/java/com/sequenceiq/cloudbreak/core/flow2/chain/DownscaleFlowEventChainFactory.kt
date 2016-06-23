package com.sequenceiq.cloudbreak.core.flow2.chain

import java.util.Optional
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.common.type.ScalingType
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class DownscaleFlowEventChainFactory : FlowEventChainFactory<ClusterAndStackDownscaleTriggerEvent> {
    @Inject
    private val stackService: StackService? = null

    @Inject
    private val hostGroupService: HostGroupService? = null

    override fun initEvent(): String {
        return FlowTriggers.FULL_DOWNSCALE_TRIGGER_EVENT
    }

    override fun createFlowTriggerEventQueue(event: ClusterAndStackDownscaleTriggerEvent): Queue<Selectable> {
        val flowEventChain = ConcurrentLinkedQueue<Selectable>()
        flowEventChain.add(ClusterScaleTriggerEvent(FlowTriggers.CLUSTER_DOWNSCALE_TRIGGER_EVENT, event.stackId, event.hostGroupName,
                event.adjustment))
        if (event.scalingType === ScalingType.DOWNSCALE_TOGETHER) {
            val stack = stackService!!.getById(event.stackId)
            val hostGroup = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, event.hostGroupName)
            val hostGroupConstraint = hostGroup.constraint
            val instanceGroupName = Optional.ofNullable(hostGroupConstraint.instanceGroup).map<String>(Function<InstanceGroup, String> { it.getGroupName() }).orElse(null)
            flowEventChain.add(StackScaleTriggerEvent(FlowTriggers.STACK_DOWNSCALE_TRIGGER_EVENT, event.stackId, instanceGroupName,
                    event.adjustment))
        }
        return flowEventChain
    }
}
