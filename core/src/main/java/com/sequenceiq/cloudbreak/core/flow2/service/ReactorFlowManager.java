package com.sequenceiq.cloudbreak.core.flow2.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.InstanceTerminationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.bus.EventBus;

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
public class ReactorFlowManager {

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    public void triggerProvisioning(Long stackId) {
        String selector = FlowChainTriggers.FULL_PROVISION_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerStackStart(Long stackId) {
        String selector = FlowChainTriggers.FULL_START_TRIGGER_EVENT;
        StackEvent startTriggerEvent = new StackEvent(selector, stackId);
        reactor.notify(selector, eventFactory.createEvent(startTriggerEvent, selector));
    }

    public void triggerStackStop(Long stackId) {
        String selector = FlowTriggers.STACK_STOP_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerStackUpscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment) {
        String selector = FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
        StackAndClusterUpscaleTriggerEvent stackAndClusterUpscaleTriggerEvent = new StackAndClusterUpscaleTriggerEvent(selector,
                stackId, instanceGroupAdjustment.getInstanceGroup(), instanceGroupAdjustment.getScalingAdjustment(),
                instanceGroupAdjustment.getWithClusterEvent() ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK);
        reactor.notify(selector, eventFactory.createEvent(stackAndClusterUpscaleTriggerEvent, selector));
    }

    public void triggerStackDownscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment) {
        String selector = FlowTriggers.STACK_DOWNSCALE_TRIGGER_EVENT;
        StackScaleTriggerEvent stackScaleTriggerEvent = new StackScaleTriggerEvent(selector, stackId, instanceGroupAdjustment.getInstanceGroup(),
                instanceGroupAdjustment.getScalingAdjustment());
        reactor.notify(selector, eventFactory.createEvent(stackScaleTriggerEvent, selector));
    }

    public void triggerStackSync(Long stackId) {
        String selector = FlowTriggers.STACK_SYNC_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackSyncTriggerEvent(selector, stackId, true), selector));
    }

    public void triggerStackRemoveInstance(Long stackId, String instanceId) {
        String selector = FlowTriggers.REMOVE_INSTANCE_TRIGGER_EVENT;
        InstanceTerminationTriggerEvent event = new InstanceTerminationTriggerEvent(selector, stackId, instanceId);
        reactor.notify(selector, eventFactory.createEvent(event, selector));
    }

    public void triggerTermination(Long stackId) {
        String selector = FlowTriggers.STACK_TERMINATE_TRIGGER_EVENT;
        StackEvent event = new StackEvent(selector, stackId);
        StackEvent cancelEvent = new StackEvent(Flow2Handler.FLOW_CANCEL, stackId);
        reactor.notify(selector, eventFactory.createEvent(event, selector));
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEvent(cancelEvent, Flow2Handler.FLOW_CANCEL));
    }

    public void triggerForcedTermination(Long stackId) {
        String selector = FlowTriggers.STACK_FORCE_TERMINATE_TRIGGER_EVENT;
        StackEvent event = new StackEvent(selector, stackId);
        reactor.notify(selector, eventFactory.createEvent(event, selector));
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEvent(event, Flow2Handler.FLOW_CANCEL));
    }

    public void triggerClusterInstall(Long stackId) {
        String selector = FlowTriggers.CLUSTER_PROVISION_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerClusterReInstall(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_RESET_CHAIN_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerClusterCredentialChange(Long stackId, String userName, String password) {
        String selector = FlowTriggers.CLUSTER_CREDENTIALCHANGE_TRIGGER_EVENT;
        ClusterCredentialChangeTriggerEvent event = new ClusterCredentialChangeTriggerEvent(selector, stackId, userName, password);
        reactor.notify(selector, eventFactory.createEvent(event, selector));
    }

    public void triggerClusterUpscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        String selector = FlowTriggers.CLUSTER_UPSCALE_TRIGGER_EVENT;
        ClusterScaleTriggerEvent event = new ClusterScaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment());
        reactor.notify(selector, eventFactory.createEvent(event, event.selector()));
    }

    public void triggerClusterDownscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
        ScalingType scalingType = hostGroupAdjustment.getWithStackUpdate() ? ScalingType.DOWNSCALE_TOGETHER : ScalingType.DOWNSCALE_ONLY_CLUSTER;
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment(), scalingType);
        reactor.notify(selector, eventFactory.createEvent(event, selector));
    }

    public void triggerClusterStart(Long stackId) {
        String selector = FlowTriggers.CLUSTER_START_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerClusterStop(Long stackId) {
        String selector = FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerClusterSync(Long stackId) {
        String selector = FlowTriggers.CLUSTER_SYNC_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerFullSync(Long stackId) {
        String selector = FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    public void triggerClusterTermination(Long stackId) {
        String selector = FlowTriggers.CLUSTER_TERMINATION_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }
}
