package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CREATING_LOAD_BALANCER;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CREATING_LOAD_BALANCER_FINISHED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.PROVISION_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_FINISHED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.configuration.LoadBalancerConfigurationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerConfigurationService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

@Configuration
public class FreeIpaLoadBalancerProvisionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerProvisionActions.class);

    @Bean(name = "CREATE_CONFIGURATION_STATE")
    public Action<?, ?> createConfiguration() {
        return new AbstractLoadBalancerCreationAction<>(StackEvent.class) {

            @Inject
            private FreeIpaLoadBalancerConfigurationService freeIpaLoadBalancerConfigurationService;

            @Inject
            private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), CREATING_LOAD_BALANCER, "Creating FreeIPA load balancer configuration");
                Long stackId = payload.getResourceId();
                if (freeIpaLoadBalancerService.findByStackId(stackId).isEmpty()) {
                    LOGGER.debug("Creating load balancer configuration for FreeIPA cluster");
                    LoadBalancer loadBalancer = freeIpaLoadBalancerConfigurationService.createLoadBalancerConfiguration(stackId, context.getStack().getName());
                    freeIpaLoadBalancerService.save(loadBalancer);
                } else {
                    LOGGER.debug("Load balancer configuration is already exists for FreeIPA cluster.");
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new LoadBalancerConfigurationSuccess(context.getStack().getId());
            }
        };
    }

    @Bean(name = "PROVISIONING_STATE")
    public Action<?, ?> loadBalancerProvision() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerConfigurationSuccess.class) {

            @Override
            protected void doExecute(StackContext context, LoadBalancerConfigurationSuccess payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), CREATING_LOAD_BALANCER, "Provisioning FreeIPA load balancer");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new LoadBalancerProvisionRequest(context.getStack().getId(), context.getCloudContext(), context.getCloudCredential(),
                        context.getCloudStack());
            }
        };
    }

    @Bean(name = "METADATA_COLLECTION_STATE")
    public Action<?, ?> loadBalancerMetadataCollection() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerProvisionSuccess.class) {

            @Override
            protected void doExecute(StackContext context, LoadBalancerProvisionSuccess payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), CREATING_LOAD_BALANCER, "Collecting load balancer metadata");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new LoadBalancerMetadataCollectionRequest(context.getStack().getId(), context.getCloudContext(), context.getCloudCredential(),
                        context.getCloudStack());
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_CREATION_FINISHED_STATE")
    public Action<?, ?> loadBalancerCreationFinished() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerMetadataCollectionSuccess.class) {

            @Override
            protected void doExecute(StackContext context, LoadBalancerMetadataCollectionSuccess payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), CREATING_LOAD_BALANCER_FINISHED, "FreeIPA load balancer creation finished");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(FREEIPA_LOAD_BALANCER_CREATION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "PROVISION_FAILED_STATE")
    public Action<?, ?> handleLoadBalancerProvisionFailure() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerCreationFailureEvent.class) {

            @Override
            protected void doExecute(StackContext context, LoadBalancerCreationFailureEvent payload, Map<Object, Object> variables) {
                String errorReason = getErrorReason(payload.getException());
                stackUpdater().updateStackStatus(context.getStack(), PROVISION_FAILED, errorReason);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(FREEIPA_LOAD_BALANCER_CREATION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }

            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaLoadBalancerProvisionState,
                    FreeIpaLoadBalancerCreationEvent> stateContext, LoadBalancerCreationFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }
        };
    }
}