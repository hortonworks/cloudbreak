package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow.FlowInitializer.Phases;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.context.TerminationContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
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

    private Map<Class, Transition> transitionMap = new HashMap();

    @Autowired
    private Reactor reactor;

    @Autowired
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    private String transitionKey(Class handler, boolean success) {
        LOGGER.debug("Transitioning from handler {}. Scenario {}", handler, success ? "SUCCESS" : "ERROR");
        String transitionKey = null;
        if (transitionMap.containsKey(handler)) {
            if (success) {
                transitionKey = transitionMap.get(handler).getNext();
            } else {
                transitionKey = transitionMap.get(handler).getFailure();
            }
            LOGGER.debug("Transitioning to [ {} ] from handler [ {} ] ", transitionKey, handler);
        } else {
            LOGGER.debug("There is no registered transition from handler {}", handler);
        }
        return transitionKey;
    }

    @Override
    public void registerTransition(Class handlerClass, Transition transition) {
        transitionMap.put(handlerClass, transition);
    }

    @Override
    public void triggerProvisioning(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        ProvisioningContext context = FlowContextFactory.createProvisioningSetupContext(provisionRequest.getCloudPlatform(),
                provisionRequest.getStackId());
        reactor.notify(Phases.PROVISIONING_SETUP.name(), eventFactory.createEvent(context, Phases.PROVISIONING_SETUP.name()));
    }

    @Override
    public void triggerNext(Class sourceHandlerClass, Object payload, boolean success) {
        String key = transitionKey(sourceHandlerClass, success);
        if (null != key) {
            Event event = eventFactory.createEvent(payload, key);
            reactor.notify(key, event);
        } else {
            LOGGER.debug("The handler {} has no transitions.", sourceHandlerClass);
        }
    }

    @Override
    public void triggerClusterInstall(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        ProvisioningContext context = FlowContextFactory.createProvisioningSetupContext(provisionRequest.getCloudPlatform(),
                provisionRequest.getStackId());
        reactor.notify(Phases.CLUSTER_CREATION.name(), eventFactory.createEvent(context, Phases.CLUSTER_CREATION.name()));
    }

    @Override
    public void triggerStackStop(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        StackStatusUpdateContext context = (StackStatusUpdateContext)
                FlowContextFactory.createStatusUpdateContext(statusUpdateRequest.getStackId(), false);
        reactor.notify(Phases.STACK_STOP.name(), eventFactory.createEvent(context, Phases.STACK_STOP.name()));
    }

    @Override
    public void triggerStackStart(Object object) {
        StackStatusUpdateRequest statusUpdateRequest = (StackStatusUpdateRequest) object;
        StackStatusUpdateContext context = (StackStatusUpdateContext)
                FlowContextFactory.createStatusUpdateContext(statusUpdateRequest.getStackId(), true);
        reactor.notify(Phases.STACK_START.name(), eventFactory.createEvent(context, Phases.STACK_START.name()));
    }

    @Override
    public void triggerClusterStop(Object object) {
        ClusterStatusUpdateRequest statusUpdateRequest = (ClusterStatusUpdateRequest) object;
        StackStatusUpdateContext context = (StackStatusUpdateContext)
                FlowContextFactory.createStatusUpdateContext(statusUpdateRequest.getStackId(), false);
        reactor.notify(Phases.CLUSTER_STOP.name(), eventFactory.createEvent(context, Phases.CLUSTER_STOP.name()));
    }

    @Override
    public void triggerClusterStart(Object object) {
        ClusterStatusUpdateRequest statusUpdateRequest = (ClusterStatusUpdateRequest) object;
        StackStatusUpdateContext context = (StackStatusUpdateContext)
                FlowContextFactory.createStatusUpdateContext(statusUpdateRequest.getStackId(), true);
        reactor.notify(Phases.CLUSTER_START.name(), eventFactory.createEvent(context, Phases.CLUSTER_START.name()));
    }

    @Override
    public void triggerTermination(Object object) {
        StackDeleteRequest deleteRequest = (StackDeleteRequest) object;
        TerminationContext context = new TerminationContext(deleteRequest.getStackId(), deleteRequest.getCloudPlatform());
        reactor.notify(Phases.TERMINATION.name(), eventFactory.createEvent(context, Phases.TERMINATION.name()));
    }

    @Override
    public void triggerStackUpscale(Object object) {
        UpdateInstancesRequest updateRequest = (UpdateInstancesRequest) object;
        StackScalingContext context = new StackScalingContext(updateRequest);
        reactor.notify(Phases.STACK_UPSCALE.name(), eventFactory.createEvent(context, Phases.STACK_UPSCALE.name()));
    }

    @Override
    public void triggerStackDownscale(Object object) {
        UpdateInstancesRequest updateRequest = (UpdateInstancesRequest) object;
        StackScalingContext context = new StackScalingContext(updateRequest);
        reactor.notify(Phases.STACK_DOWNSCALE.name(), eventFactory.createEvent(context, Phases.STACK_DOWNSCALE.name()));
    }

    @Override
    public void triggerClusterUpscale(Object object) {
        UpdateAmbariHostsRequest request = (UpdateAmbariHostsRequest) object;
        ClusterScalingContext context = new ClusterScalingContext(request);
        reactor.notify(Phases.CLUSTER_UPSCALE.name(), eventFactory.createEvent(context, Phases.CLUSTER_UPSCALE.name()));
    }

    @Override
    public void triggerClusterDownscale(Object object) {
        UpdateAmbariHostsRequest request = (UpdateAmbariHostsRequest) object;
        ClusterScalingContext context = new ClusterScalingContext(request);
        reactor.notify(Phases.CLUSTER_DOWNSCALE.name(), eventFactory.createEvent(context, Phases.CLUSTER_DOWNSCALE.name()));
    }

    public static class TransitionFactory {
        public static Transition createTransition(String current, String next, String failure) {
            return new Transition(current, next, failure);
        }
    }

}

