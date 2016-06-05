package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow.ErrorHandlerAwareFlowEventFactory;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterCredentialChangeEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.StartInstanceTerminationEvent;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterDeleteRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterUserNamePasswordUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackForcedDeleteRequest;

import reactor.bus.EventBus;

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
public class ReactorFlowManager implements FlowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorFlowManager.class);

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @Override
    public void triggerProvisioning(Long stackId) {
        String selector = FlowTriggers.FULL_PROVISION_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerStackStart(Long stackId) {
        String selector = FlowTriggers.FULL_START_TRIGGER_EVENT;
        StackEvent startTriggerEvent = new StackEvent(selector, stackId);
        reactor.notify(selector, eventFactory.createEvent(startTriggerEvent, selector));
    }

    @Override
    public void triggerStackStop(Long stackId) {
        String selector = FlowTriggers.STACK_STOP_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerStackUpscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment) {
        String selector = FlowTriggers.FULL_UPSCALE_TRIGGER_EVENT;
        StackAndClusterUpscaleTriggerEvent stackAndClusterUpscaleTriggerEvent = new StackAndClusterUpscaleTriggerEvent(selector,
                stackId, instanceGroupAdjustment.getInstanceGroup(), instanceGroupAdjustment.getScalingAdjustment(),
                instanceGroupAdjustment.getWithClusterEvent() ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK);
        reactor.notify(selector, eventFactory.createEvent(stackAndClusterUpscaleTriggerEvent, selector));
    }

    @Override
    public void triggerStackDownscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment) {
        String selector = FlowTriggers.STACK_DOWNSCALE_TRIGGER_EVENT;
        StackScaleTriggerEvent stackScaleTriggerEvent = new StackScaleTriggerEvent(selector, stackId, instanceGroupAdjustment.getInstanceGroup(),
                instanceGroupAdjustment.getScalingAdjustment());
        reactor.notify(selector, eventFactory.createEvent(stackScaleTriggerEvent, selector));
    }

    @Override
    public void triggerStackSync(Long stackId) {
        String selector = FlowTriggers.STACK_SYNC_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerClusterInstall(Long stackId) {
        String selector = FlowTriggers.CLUSTER_PROVISION_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerClusterUpscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        String selector = FlowTriggers.CLUSTER_UPSCALE_TRIGGER_EVENT;
        ClusterScaleTriggerEvent event = new ClusterScaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment());
        reactor.notify(selector, eventFactory.createEvent(event, event.selector()));
    }

    @Override
    public void triggerClusterDownscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        String selector = FlowTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
        ScalingType scalingType = hostGroupAdjustment.getWithStackUpdate() ? ScalingType.DOWNSCALE_TOGETHER : ScalingType.DOWNSCALE_ONLY_CLUSTER;
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment(), scalingType);
        reactor.notify(selector, eventFactory.createEvent(event, selector));
    }

    @Override
    public void triggerClusterStart(Long stackId) {
        String selector = FlowTriggers.CLUSTER_START_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerClusterStop(Long stackId) {
        String selector = FlowTriggers.FULL_STOP_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerClusterSync(Long stackId) {
        String selector = FlowTriggers.CLUSTER_SYNC_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerFullSync(Long stackId) {
        String selector = FlowTriggers.FULL_SYNC_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEvent(new StackEvent(selector, stackId), selector));
    }

    @Override
    public void triggerClusterReInstall(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        reactor.notify(FlowPhases.CLUSTER_RESET.name(),
                eventFactory.createEvent(new StackEvent(provisionRequest.getStackId()), FlowPhases.CLUSTER_RESET.name()));
    }

    @Override
    public void triggerTermination(Object object) {
        StackDeleteRequest deleteRequest = (StackDeleteRequest) object;
        StackEvent event = new StackEvent(deleteRequest.getStackId());
        reactor.notify(FlowPhases.TERMINATION.name(), eventFactory.createEvent(event, FlowPhases.TERMINATION.name()));
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEvent(event, Flow2Handler.FLOW_CANCEL));
    }

    @Override
    public void triggerForcedTermination(Object object) {
        StackDeleteRequest deleteRequest = (StackForcedDeleteRequest) object;
        StackEvent event = new StackEvent(deleteRequest.getStackId());
        reactor.notify(FlowPhases.FORCED_TERMINATION.name(), eventFactory.createEvent(event, FlowPhases.FORCED_TERMINATION.name()));
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEvent(event, Flow2Handler.FLOW_CANCEL));
    }

    @Override
    public void triggerClusterTermination(Object object) {
        ClusterDeleteRequest deleteRequest = (ClusterDeleteRequest) object;
        reactor.notify(FlowPhases.CLUSTER_TERMINATION.name(),
                eventFactory.createEvent(new StackEvent(deleteRequest.getStackId()), FlowPhases.CLUSTER_TERMINATION.name()));
    }

    @Override
    public void triggerStackRemoveInstance(Object object) {
        RemoveInstanceRequest removeInstanceRequest = (RemoveInstanceRequest) object;
        StartInstanceTerminationEvent event = new StartInstanceTerminationEvent(removeInstanceRequest.getStackId(), removeInstanceRequest.getInstanceId());
        reactor.notify(FlowPhases.REMOVE_INSTANCE.name(), eventFactory.createEvent(event, FlowPhases.REMOVE_INSTANCE.name()));
    }

    @Override
    public void triggerClusterUserNamePasswordUpdate(Object object) {
        ClusterUserNamePasswordUpdateRequest request = (ClusterUserNamePasswordUpdateRequest) object;
        StartClusterCredentialChangeEvent event = new StartClusterCredentialChangeEvent(request.getStackId(), request.getNewUserName(),
                    request.getNewPassword());
        reactor.notify(FlowPhases.CLUSTER_USERNAME_PASSWORD_UPDATE.name(),
                eventFactory.createEvent(event, FlowPhases.CLUSTER_USERNAME_PASSWORD_UPDATE.name()));
    }

}

