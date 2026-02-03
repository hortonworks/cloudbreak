package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.ADDING_LOAD_BALANCER_FINISHED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CREATING_LOAD_BALANCER_FINISHED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.PROVISION_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.PROVISION_VALIDATION_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_VALIDATION_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_FINISHED_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.StackEventToLoadBalancerCreationTriggerEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.configuration.LoadBalancerConfigurationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerConfigurationService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

@Configuration
public class FreeIpaLoadBalancerProvisionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerProvisionActions.class);

    @Bean(name = "CREATE_CONFIGURATION_STATE")
    public Action<?, ?> createConfiguration() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerCreationTriggerEvent.class) {

            @Inject
            private FreeIpaLoadBalancerConfigurationService freeIpaLoadBalancerConfigurationService;

            @Inject
            private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

            @Override
            protected void prepareExecution(LoadBalancerCreationTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(LOAD_BALANCER_PROVISIONING_MODE, payload.getLoadBalancerProvisioningMode());
            }

            @Override
            protected void doExecute(StackContext context, LoadBalancerCreationTriggerEvent payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), getInProgressState(variables), "Creating FreeIPA load balancer configuration");
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

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<LoadBalancerCreationTriggerEvent>> payloadConverters) {
                payloadConverters.add(new StackEventToLoadBalancerCreationTriggerEventConverter());
            }
        };
    }

    @Bean(name = "PROVISIONING_STATE")
    public Action<?, ?> loadBalancerProvision() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerConfigurationSuccess.class) {

            @Override
            protected void doExecute(StackContext context, LoadBalancerConfigurationSuccess payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), getInProgressState(variables), "Provisioning FreeIPA load balancer");
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
                stackUpdater().updateStackStatus(context.getStack(), getInProgressState(variables), "Collecting load balancer metadata");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new LoadBalancerMetadataCollectionRequest(context.getStack().getId(), context.getCloudContext(), context.getCloudCredential(),
                        context.getCloudStack());
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_DOMAIN_UPDATE_STATE")
    public Action<?, ?> loadBalancerDomainUpdate() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerMetadataCollectionSuccess.class) {

            @Override
            protected void doExecute(StackContext context, LoadBalancerMetadataCollectionSuccess payload, Map<Object, Object> variables) {
                if (isBootstrapMode(variables)) {
                    sendEvent(context, new LoadBalancerDomainUpdateSuccess(payload.getResourceId()));
                } else {
                    stackUpdater().updateStackStatus(context.getStack(), getInProgressState(variables), "Updating FreeIPA load balancer domain");
                    sendEvent(context, new LoadBalancerDomainUpdateRequest(payload.getResourceId()));
                }
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_CREATION_FINISHED_STATE")
    public Action<?, ?> loadBalancerCreationFinished() {
        return new AbstractLoadBalancerCreationAction<>(LoadBalancerDomainUpdateSuccess.class) {

            @Override
            protected void doExecute(StackContext context, LoadBalancerDomainUpdateSuccess payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(),
                        isBootstrapMode(variables) ? CREATING_LOAD_BALANCER_FINISHED : ADDING_LOAD_BALANCER_FINISHED,
                        "FreeIPA load balancer creation finished");
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

            @Inject
            private FreeIpaFailedFlowAnalyzer freeIpaFailedFlowAnalyzer;

            @Override
            protected void doExecute(StackContext context, LoadBalancerCreationFailureEvent payload, Map<Object, Object> variables) {
                String errorReason = getErrorReason(payload.getException());
                DetailedStackStatus detailedStackStatus = calculateFailureStatus(payload, variables);
                stackUpdater().updateStackStatus(context.getStack(), detailedStackStatus, errorReason);
                sendEvent(context);
            }

            private DetailedStackStatus calculateFailureStatus(LoadBalancerCreationFailureEvent payload, Map<Object, Object> variables) {
                if (freeIpaFailedFlowAnalyzer.isValidationFailedError(payload)) {
                    return isBootstrapMode(variables) ? PROVISION_VALIDATION_FAILED : UPGRADE_VALIDATION_FAILED;
                } else {
                    return isBootstrapMode(variables) ? PROVISION_FAILED : UPGRADE_FAILED;
                }
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(FREEIPA_LOAD_BALANCER_CREATION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}