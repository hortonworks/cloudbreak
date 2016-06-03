package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow.ErrorHandlerAwareFlowEventFactory;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.TransitionKeyService;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterCredentialChangeEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterScaleEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.StartInstanceTerminationEvent;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterDeleteRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterUserNamePasswordUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackForcedDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

import reactor.bus.Event;
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
    private TransitionKeyService transitionKeyService;

    @Inject
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @Override
    public void triggerNext(Class sourceHandlerClass, Object payload, boolean success) {
        String key = success ? transitionKeyService.successKey(sourceHandlerClass) : transitionKeyService.failureKey(sourceHandlerClass);
        if (isTriggerKey(key)) {
            Event event = eventFactory.createEvent(payload, key);
            reactor.notify(key, event);
        } else {
            LOGGER.debug("The handler {} has no transitions.", sourceHandlerClass);
        }
    }

    @Override
    public void triggerProvisioning(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        reactor.notify(FlowPhases.STACKANDCLUSTER_PROVISIONING_SETUP.name(), Event.wrap(provisionRequest));
    }

    private boolean isTriggerKey(String key) {
        return key != null && !FlowPhases.valueOf(key).equals(FlowPhases.NONE);
    }

    @Override
    public void triggerClusterInstall(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        reactor.notify(FlowPhases.RUN_CLUSTER_CONTAINERS.name(),
                eventFactory.createEvent(new StackEvent(provisionRequest.getStackId()), FlowPhases.RUN_CLUSTER_CONTAINERS.name()));
    }

    @Override
    public void triggerClusterReInstall(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        reactor.notify(FlowPhases.CLUSTER_RESET.name(),
                eventFactory.createEvent(new StackEvent(provisionRequest.getStackId()), FlowPhases.CLUSTER_RESET.name()));
    }

    @Override
    public void triggerStackStop(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        reactor.notify(FlowPhases.STACK_STOP.name(),
                eventFactory.createEvent(new StackEvent(statusUpdateRequest.getStackId()), FlowPhases.STACK_STOP.name()));
    }

    @Override
    public void triggerStackStart(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        reactor.notify(FlowPhases.STACK_START.name(),
                eventFactory.createEvent(new StackEvent(statusUpdateRequest.getStackId()), FlowPhases.STACK_START.name()));
    }

    @Override
    public void triggerClusterStop(Object object) {
        ClusterStatusUpdateRequest statusUpdateRequest = (ClusterStatusUpdateRequest) object;
        reactor.notify(FlowPhases.CLUSTER_AND_STACK_STOP.name(),
                eventFactory.createEvent(new StackEvent(statusUpdateRequest.getStackId()), FlowPhases.CLUSTER_AND_STACK_STOP.name()));
    }

    @Override
    public void triggerClusterStart(Object object) {
        ClusterStatusUpdateRequest statusUpdateRequest = (ClusterStatusUpdateRequest) object;
        reactor.notify(FlowPhases.CLUSTER_START.name(),
                eventFactory.createEvent(new StackStatusUpdateContext(statusUpdateRequest.getStackId(), statusUpdateRequest.getCloudPlatform(), true),
                        FlowPhases.CLUSTER_START.name()));
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
    public void triggerStackUpscale(Object object) {
        UpdateInstancesRequest updateRequest = (UpdateInstancesRequest) object;
        StackScalingContext context = new StackScalingContext(updateRequest);
        reactor.notify(FlowPhases.UPSCALE_STACK_SYNC.name(), eventFactory.createEvent(context, FlowPhases.UPSCALE_STACK_SYNC.name()));
    }

    @Override
    public void triggerStackDownscale(Object object) {
        UpdateInstancesRequest updateRequest = (UpdateInstancesRequest) object;
        StackScalingContext context = new StackScalingContext(updateRequest);
        reactor.notify(FlowPhases.STACK_DOWNSCALE.name(), eventFactory.createEvent(context, FlowPhases.STACK_DOWNSCALE.name()));
    }

    @Override
    public void triggerStackRemoveInstance(Object object) {
        RemoveInstanceRequest removeInstanceRequest = (RemoveInstanceRequest) object;
        StartInstanceTerminationEvent event = new StartInstanceTerminationEvent(removeInstanceRequest.getStackId(), removeInstanceRequest.getInstanceId());
        reactor.notify(FlowPhases.REMOVE_INSTANCE.name(), eventFactory.createEvent(event, FlowPhases.REMOVE_INSTANCE.name()));
    }

    @Override
    public void triggerClusterUpscale(Object object) {
        UpdateAmbariHostsRequest request = (UpdateAmbariHostsRequest) object;
        StartClusterScaleEvent event = new StartClusterScaleEvent(request.getStackId(),
                request.getHostGroupAdjustment().getHostGroup(), request.getHostGroupAdjustment().getScalingAdjustment());
        reactor.notify(FlowPhases.ADD_CLUSTER_CONTAINERS.name(), eventFactory.createEvent(event, FlowPhases.ADD_CLUSTER_CONTAINERS.name()));
    }

    @Override
    public void triggerClusterDownscale(Object object) {
        UpdateAmbariHostsRequest request = (UpdateAmbariHostsRequest) object;
        StartClusterScaleEvent event = new StartClusterScaleEvent(request.getStackId(),
                request.getHostGroupAdjustment().getHostGroup(), request.getHostGroupAdjustment().getScalingAdjustment());
        FlowPhases phase =
                ScalingType.DOWNSCALE_ONLY_CLUSTER == request.getScalingType() ? FlowPhases.CLUSTER_DOWNSCALE : FlowPhases.CLUSTER_AND_STACK_DOWNSCALE;
        reactor.notify(phase.name(), eventFactory.createEvent(event, phase.name()));
    }

    @Override
    public void triggerClusterSync(Object object) {
        ClusterStatusUpdateRequest statusUpdateRequest = (ClusterStatusUpdateRequest) object;
        reactor.notify(FlowPhases.CLUSTER_SYNC.name(),
                eventFactory.createEvent(new StackEvent(statusUpdateRequest.getStackId()), FlowPhases.CLUSTER_SYNC.name()));
    }

    @Override
    public void triggerStackSync(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        reactor.notify(FlowPhases.STACK_SYNC.name(),
                eventFactory.createEvent(new StackEvent(statusUpdateRequest.getStackId()), FlowPhases.STACK_SYNC.name()));
    }

    @Override
    public void triggerFullSync(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        reactor.notify(FlowPhases.STACK_AND_CLUSTER_SYNC.name(),
                eventFactory.createEvent(new StackEvent(statusUpdateRequest.getStackId()), FlowPhases.STACK_AND_CLUSTER_SYNC.name()));
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

