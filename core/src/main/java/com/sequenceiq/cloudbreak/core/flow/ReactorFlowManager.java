package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateAllowedSubnetsContext;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

import reactor.core.Reactor;
import reactor.event.Event;

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
public class ReactorFlowManager implements FlowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorFlowManager.class);

    @Autowired
    private Reactor reactor;

    @Autowired
    private TransitionKeyService transitionKeyService;

    @Autowired
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
        ProvisioningContext context = new ProvisioningContext.Builder().
                setDefaultParams(provisionRequest.getStackId(), provisionRequest.getCloudPlatform()).build();
        reactor.notify(FlowPhases.PROVISIONING_SETUP.name(), eventFactory.createEvent(context, FlowPhases.PROVISIONING_SETUP.name()));
    }

    private boolean isTriggerKey(String key) {
        return key != null && !FlowPhases.valueOf(key).equals(FlowPhases.NONE);
    }

    @Override
    public void triggerClusterInstall(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        ProvisioningContext context = new ProvisioningContext.Builder().
                setDefaultParams(provisionRequest.getStackId(), provisionRequest.getCloudPlatform()).build();
        reactor.notify(FlowPhases.RUN_CLUSTER_CONTAINERS.name(), eventFactory.createEvent(context, FlowPhases.RUN_CLUSTER_CONTAINERS.name()));
    }

    @Override
    public void triggerClusterReInstall(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        ProvisioningContext context = new ProvisioningContext.Builder().
                setDefaultParams(provisionRequest.getStackId(), provisionRequest.getCloudPlatform()).build();
        reactor.notify(FlowPhases.CLUSTER_RESET.name(), eventFactory.createEvent(context, FlowPhases.CLUSTER_RESET.name()));
    }

    @Override
    public void triggerStackStop(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        StackStatusUpdateContext context = new StackStatusUpdateContext(statusUpdateRequest.getStackId(), statusUpdateRequest.getCloudPlatform(), false);
        reactor.notify(FlowPhases.STACK_STOP.name(), eventFactory.createEvent(context, FlowPhases.STACK_STOP.name()));
    }

    @Override
    public void triggerStackStart(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        StackStatusUpdateContext context = new StackStatusUpdateContext(statusUpdateRequest.getStackId(), statusUpdateRequest.getCloudPlatform(), true);
        reactor.notify(FlowPhases.STACK_START.name(), eventFactory.createEvent(context, FlowPhases.STACK_START.name()));
    }

    @Override
    public void triggerClusterStop(Object object) {
        ClusterStatusUpdateRequest statusUpdateRequest = (ClusterStatusUpdateRequest) object;
        StackStatusUpdateContext context = new StackStatusUpdateContext(statusUpdateRequest.getStackId(), statusUpdateRequest.getCloudPlatform(), false);
        reactor.notify(FlowPhases.CLUSTER_STOP.name(), eventFactory.createEvent(context, FlowPhases.CLUSTER_STOP.name()));
    }

    @Override
    public void triggerClusterStart(Object object) {
        ClusterStatusUpdateRequest statusUpdateRequest = (ClusterStatusUpdateRequest) object;
        StackStatusUpdateContext context = new StackStatusUpdateContext(statusUpdateRequest.getStackId(), statusUpdateRequest.getCloudPlatform(), true);
        reactor.notify(FlowPhases.CLUSTER_START.name(), eventFactory.createEvent(context, FlowPhases.CLUSTER_START.name()));
    }

    @Override
    public void triggerTermination(Object object) {
        StackDeleteRequest deleteRequest = (StackDeleteRequest) object;
        DefaultFlowContext context = new DefaultFlowContext(deleteRequest.getStackId(), deleteRequest.getCloudPlatform());
        reactor.notify(FlowPhases.TERMINATION.name(), eventFactory.createEvent(context, FlowPhases.TERMINATION.name()));
    }

    @Override
    public void triggerStackUpscale(Object object) {
        UpdateInstancesRequest updateRequest = (UpdateInstancesRequest) object;
        StackScalingContext context = new StackScalingContext(updateRequest);
        reactor.notify(FlowPhases.ADD_INSTANCES.name(), eventFactory.createEvent(context, FlowPhases.ADD_INSTANCES.name()));
    }

    @Override
    public void triggerStackDownscale(Object object) {
        UpdateInstancesRequest updateRequest = (UpdateInstancesRequest) object;
        StackScalingContext context = new StackScalingContext(updateRequest);
        reactor.notify(FlowPhases.STACK_DOWNSCALE.name(), eventFactory.createEvent(context, FlowPhases.STACK_DOWNSCALE.name()));
    }

    @Override
    public void triggerClusterUpscale(Object object) {
        UpdateAmbariHostsRequest request = (UpdateAmbariHostsRequest) object;
        ClusterScalingContext context = new ClusterScalingContext(request);
        reactor.notify(FlowPhases.ADD_CLUSTER_CONTAINERS.name(), eventFactory.createEvent(context, FlowPhases.ADD_CLUSTER_CONTAINERS.name()));
    }

    @Override
    public void triggerClusterDownscale(Object object) {
        UpdateAmbariHostsRequest request = (UpdateAmbariHostsRequest) object;
        ClusterScalingContext context = new ClusterScalingContext(request);
        reactor.notify(FlowPhases.CLUSTER_DOWNSCALE.name(), eventFactory.createEvent(context, FlowPhases.CLUSTER_DOWNSCALE.name()));
    }

    @Override
    public void triggerUpdateAllowedSubnets(Object object) {
        UpdateAllowedSubnetsRequest updateAllowedSubnetsRequest = (UpdateAllowedSubnetsRequest) object;
        UpdateAllowedSubnetsContext context = new UpdateAllowedSubnetsContext(updateAllowedSubnetsRequest);
        reactor.notify(FlowPhases.UPDATE_ALLOWED_SUBNETS.name(), eventFactory.createEvent(context, FlowPhases.UPDATE_ALLOWED_SUBNETS.name()));
    }

}

