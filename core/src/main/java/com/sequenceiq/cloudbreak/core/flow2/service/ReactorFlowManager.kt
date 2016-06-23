package com.sequenceiq.cloudbreak.core.flow2.service

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.common.type.ScalingType
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.InstanceTerminationTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

import reactor.bus.EventBus

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
class ReactorFlowManager {

    @Inject
    private val reactor: EventBus? = null

    @Inject
    private val eventFactory: ErrorHandlerAwareFlowEventFactory? = null

    fun triggerProvisioning(stackId: Long?) {
        val selector = FlowTriggers.FULL_PROVISION_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerStackStart(stackId: Long?) {
        val selector = FlowTriggers.FULL_START_TRIGGER_EVENT
        val startTriggerEvent = StackEvent(selector, stackId)
        reactor!!.notify(selector, eventFactory!!.createEvent(startTriggerEvent, selector))
    }

    fun triggerStackStop(stackId: Long?) {
        val selector = FlowTriggers.STACK_STOP_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerStackUpscale(stackId: Long?, instanceGroupAdjustment: InstanceGroupAdjustmentJson) {
        val selector = FlowTriggers.FULL_UPSCALE_TRIGGER_EVENT
        val stackAndClusterUpscaleTriggerEvent = StackAndClusterUpscaleTriggerEvent(selector,
                stackId, instanceGroupAdjustment.instanceGroup, instanceGroupAdjustment.scalingAdjustment,
                if (instanceGroupAdjustment.withClusterEvent) ScalingType.UPSCALE_TOGETHER else ScalingType.UPSCALE_ONLY_STACK)
        reactor!!.notify(selector, eventFactory!!.createEvent(stackAndClusterUpscaleTriggerEvent, selector))
    }

    fun triggerStackDownscale(stackId: Long?, instanceGroupAdjustment: InstanceGroupAdjustmentJson) {
        val selector = FlowTriggers.STACK_DOWNSCALE_TRIGGER_EVENT
        val stackScaleTriggerEvent = StackScaleTriggerEvent(selector, stackId, instanceGroupAdjustment.instanceGroup,
                instanceGroupAdjustment.scalingAdjustment)
        reactor!!.notify(selector, eventFactory!!.createEvent(stackScaleTriggerEvent, selector))
    }

    fun triggerStackSync(stackId: Long?) {
        val selector = FlowTriggers.STACK_SYNC_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackSyncTriggerEvent(selector, stackId, true), selector))
    }

    fun triggerStackRemoveInstance(stackId: Long?, instanceId: String) {
        val selector = FlowTriggers.REMOVE_INSTANCE_TRIGGER_EVENT
        val event = InstanceTerminationTriggerEvent(selector, stackId, instanceId)
        reactor!!.notify(selector, eventFactory!!.createEvent(event, selector))
    }

    fun triggerTermination(stackId: Long?) {
        val selector = FlowTriggers.STACK_TERMINATE_TRIGGER_EVENT
        val event = StackEvent(selector, stackId)
        val cancelEvent = StackEvent(Flow2Handler.FLOW_CANCEL, stackId)
        reactor!!.notify(selector, eventFactory!!.createEvent(event, selector))
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEvent(cancelEvent, Flow2Handler.FLOW_CANCEL))
    }

    fun triggerForcedTermination(stackId: Long?) {
        val selector = FlowTriggers.STACK_FORCE_TERMINATE_TRIGGER_EVENT
        val event = StackEvent(selector, stackId)
        val cancelEvent = StackEvent(Flow2Handler.FLOW_CANCEL, stackId)
        reactor!!.notify(selector, eventFactory!!.createEvent(event, selector))
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEvent(event, Flow2Handler.FLOW_CANCEL))
    }

    fun triggerClusterInstall(stackId: Long?) {
        val selector = FlowTriggers.CLUSTER_PROVISION_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerClusterReInstall(stackId: Long?) {
        val selector = FlowTriggers.CLUSTER_RESET_CHAIN_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerClusterCredentialChange(stackId: Long?, userName: String, password: String) {
        val selector = FlowTriggers.CLUSTER_CREDENTIALCHANGE_TRIGGER_EVENT
        val event = ClusterCredentialChangeTriggerEvent(selector, stackId, userName, password)
        reactor!!.notify(selector, eventFactory!!.createEvent(event, selector))
    }

    fun triggerClusterUpscale(stackId: Long?, hostGroupAdjustment: HostGroupAdjustmentJson) {
        val selector = FlowTriggers.CLUSTER_UPSCALE_TRIGGER_EVENT
        val event = ClusterScaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.hostGroup, hostGroupAdjustment.scalingAdjustment)
        reactor!!.notify(selector, eventFactory!!.createEvent(event, event.selector()))
    }

    fun triggerClusterDownscale(stackId: Long?, hostGroupAdjustment: HostGroupAdjustmentJson) {
        val selector = FlowTriggers.FULL_DOWNSCALE_TRIGGER_EVENT
        val scalingType = if (hostGroupAdjustment.withStackUpdate) ScalingType.DOWNSCALE_TOGETHER else ScalingType.DOWNSCALE_ONLY_CLUSTER
        val event = ClusterAndStackDownscaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.hostGroup, hostGroupAdjustment.scalingAdjustment, scalingType)
        reactor!!.notify(selector, eventFactory!!.createEvent(event, selector))
    }

    fun triggerClusterStart(stackId: Long?) {
        val selector = FlowTriggers.CLUSTER_START_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerClusterStop(stackId: Long?) {
        val selector = FlowTriggers.FULL_STOP_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerClusterSync(stackId: Long?) {
        val selector = FlowTriggers.CLUSTER_SYNC_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerFullSync(stackId: Long?) {
        val selector = FlowTriggers.FULL_SYNC_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }

    fun triggerClusterTermination(stackId: Long?) {
        val selector = FlowTriggers.CLUSTER_TERMINATION_TRIGGER_EVENT
        reactor!!.notify(selector, eventFactory!!.createEvent(StackEvent(selector, stackId), selector))
    }
}

