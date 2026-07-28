package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_ROLLING_VERTICAL_SCALE_HEALTH_CHECKING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_ROLLING_VERTICAL_SCALE_INSTANCE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_ROLLING_VERTICAL_SCALE_INSTANCE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_ROLLING_VERTICAL_SCALE_RESIZING_INSTANCE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_ROLLING_VERTICAL_SCALE_STARTING_INSTANCE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_ROLLING_VERTICAL_SCALE_STOPPING_SERVICES;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.VERTICAL_SCALE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.VERTICAL_SCALE_IN_PROGRESS;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_FINISHED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stophealthagent.StopHealthAgentRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeResult;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeResult;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StopFreeIpaServicesEvent;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Configuration
public class FreeIpaRollingVerticalScaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRollingVerticalScaleActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private OperationService operationService;

    @Bean(name = "ROLLING_VERTICAL_SCALE_DESCRIBE_STATE")
    public Action<?, ?> describeAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(FreeIpaRollingVerticalScaleTriggerEvent.class) {

            @Override
            protected void prepareExecution(FreeIpaRollingVerticalScaleTriggerEvent payload, Map<Object, Object> variables) {
                setInstanceIds(variables, List.of(payload.getInstanceId()));
                variables.put(SCALE_CONFIG_VAR, payload.getScaleConfig());
                setFinalChain(variables, payload.isFinalChain());
                setOperationId(variables, payload.getOperationId());
            }

            @Override
            protected void doExecute(StackContext context, FreeIpaRollingVerticalScaleTriggerEvent payload, Map<Object, Object> variables) {
                String instanceId = payload.getInstanceId();
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_IN_PROGRESS,
                        "Rolling vertical scale: describing instance type for " + instanceId);
                sendEvent(context, new RollingVerticalScaleDescribeRequest(payload.getResourceId(), instanceId, payload.getScaleConfig()));
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE")
    public Action<?, ?> stopHealthAgentAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(RollingVerticalScaleDescribeResult.class) {

            @Inject
            private FreeIpaLoadBalancerService loadBalancerService;

            @Override
            protected void prepareExecution(RollingVerticalScaleDescribeResult payload, Map<Object, Object> variables) {
                variables.put(SCALE_CONFIG_VAR, payload.getScaleConfig());
            }

            @Override
            protected void doExecute(StackContext context, RollingVerticalScaleDescribeResult payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_IN_PROGRESS,
                        "Rolling vertical scale: stopping health agent on instance " + instanceId);
                if (loadBalancerService.findByStackId(payload.getResourceId()).isPresent()) {
                    LOGGER.debug("Load balancer present, stopping health agent on instance {} before FreeIPA services stop", instanceId);
                    String fqdn = instanceMetaDataService.getByInstanceIds(payload.getResourceId(), List.of(instanceId))
                            .stream().findFirst().orElseThrow().getDiscoveryFQDN();
                    sendEvent(context, new StopHealthAgentRequest(payload.getResourceId(), List.of(fqdn)));
                } else {
                    sendEvent(context, new StackEvent(ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_FINISHED_EVENT.event(), payload.getResourceId()));
                }
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE")
    public Action<?, ?> stopServicesAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_IN_PROGRESS,
                        "Rolling vertical scale in progress on instance " + instanceId);
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_ROLLING_VERTICAL_SCALE_STOPPING_SERVICES, List.of(instanceId));
                sendEvent(context, new StopFreeIpaServicesEvent(payload.getResourceId(), List.of(instanceId)));
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_STOP_VM_STATE")
    public Action<?, ?> stopVmAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_IN_PROGRESS,
                        "Stopping VM instance " + instanceId + " for rolling vertical scale");
                CloudInstance cloudInstance = getCloudInstance(context, instanceId);
                List<CloudResource> cloudResources = resourceService.getAllCloudResource(payload.getResourceId());
                sendEvent(context, new StopInstancesRequest(
                        context.getCloudContext(), context.getCloudCredential(),
                        cloudResources, List.of(cloudInstance)));
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_RESIZE_STATE")
    public Action<?, ?> resizeAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(StopInstancesResult.class) {
            @Override
            protected void doExecute(StackContext context, StopInstancesResult payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                instanceMetaDataService.updateStatus(context.getStack(), List.of(instanceId), InstanceStatus.STOPPED);
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_IN_PROGRESS,
                        "Resizing instance " + instanceId + " for rolling vertical scale");
                FreeIpaVerticalScaleParameters scaleConfig = getScaleConfig(variables);
                String targetInstanceType = scaleConfig.getTargetInstanceType();
                String originalInstanceType = scaleConfig.getOriginalInstanceType();
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_ROLLING_VERTICAL_SCALE_RESIZING_INSTANCE, List.of(instanceId, originalInstanceType, targetInstanceType));
                List<CloudResource> cloudResources = resourceService.getAllCloudResource(payload.getResourceId());
                CloudStack filteredCloudStack = filterCloudStackForInstance(context.getCloudStack(), instanceId);
                sendEvent(context, new RollingVerticalScaleResizeRequest(
                        payload.getResourceId(), context.getCloudContext(), context.getCloudCredential(),
                        filteredCloudStack, cloudResources, scaleConfig.getGroup()));
            }

            private CloudStack filterCloudStackForInstance(CloudStack cloudStack, String instanceId) {
                List<Group> filteredGroups = cloudStack.getGroups().stream()
                        .map(group -> {
                            List<CloudInstance> filtered = group.getInstances().stream()
                                    .filter(i -> instanceId.equals(i.getInstanceId()))
                                    .toList();
                            if (filtered.isEmpty()) {
                                return null;
                            }
                            return Group.builder()
                                    .withName(group.getName())
                                    .withType(group.getType())
                                    .withInstances(filtered)
                                    .withSecurity(group.getSecurity())
                                    .withParameters(group.getParameters())
                                    .withInstanceAuthentication(group.getInstanceAuthentication())
                                    .withLoginUserName(group.getLoginUserName())
                                    .withPublicKey(group.getPublicKey())
                                    .withRootVolumeSize(group.getRootVolumeSize())
                                    .withIdentity(group.getIdentity())
                                    .withDeletedInstances(group.getDeletedInstances())
                                    .withNetwork(group.getNetwork())
                                    .withTags(group.getTags())
                                    .withRootVolumeType(group.getRootVolumeType())
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .toList();
                LOGGER.debug("Filtered CloudStack for resize: keeping only instance {} across {} group(s)", instanceId, filteredGroups.size());
                return cloudStack.toBuilder().groups(filteredGroups).build();
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_START_VM_STATE")
    public Action<?, ?> startVmAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(RollingVerticalScaleResizeResult.class) {
            @Override
            protected void doExecute(StackContext context, RollingVerticalScaleResizeResult payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_IN_PROGRESS,
                        "Starting VM instance " + instanceId + " after resize");
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_ROLLING_VERTICAL_SCALE_STARTING_INSTANCE, List.of(instanceId));
                CloudInstance cloudInstance = getCloudInstance(context, instanceId);
                List<CloudResource> cloudResources = resourceService.getAllCloudResource(payload.getResourceId());
                sendEvent(context, new StartInstancesRequest(
                        context.getCloudContext(), context.getCloudCredential(),
                        cloudResources, List.of(cloudInstance)));
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE")
    public Action<?, ?> healthCheckAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(StartInstancesResult.class) {
            @Override
            protected void doExecute(StackContext context, StartInstancesResult payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_IN_PROGRESS,
                        "Health-checking instance " + instanceId + " after resize");
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_ROLLING_VERTICAL_SCALE_HEALTH_CHECKING, List.of(instanceId));
                sendEvent(context, new RollingVerticalScaleHealthCheckRequest(
                        payload.getResourceId(), instanceId));
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                instanceMetaDataService.updateStatus(context.getStack(), List.of(instanceId), InstanceStatus.CREATED);
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_ROLLING_VERTICAL_SCALE_INSTANCE_FINISHED, List.of(instanceId));
                if (isFinalChain(variables)) {
                    stackUpdater.updateStackStatus(context.getStack().getId(), AVAILABLE, "Rolling vertical scale completed successfully");
                }
                if (shouldCompleteOperation(variables) && isOperationIdSet(variables)) {
                    SuccessDetails successDetails = new SuccessDetails(context.getStack().getEnvironmentCrn());
                    operationService.completeOperation(context.getStack().getAccountId(), getOperationId(variables),
                            List.of(successDetails), List.of());
                }
                sendEvent(context, ROLLING_VERTICAL_SCALE_FINALIZED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ROLLING_VERTICAL_SCALE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractFreeIpaRollingVerticalScaleAction<>(FreeIpaRollingVerticalScaleFailureEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaRollingVerticalScaleFailureEvent payload, Map<Object, Object> variables) {
                String instanceId = getInstanceIds(variables).stream().findFirst().orElseThrow();
                String errorMessage = "Rolling vertical scale failed: " + payload.getException().getMessage();
                LOGGER.error(errorMessage, payload.getException());
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_ROLLING_VERTICAL_SCALE_INSTANCE_FAILED, List.of(instanceId, payload.getException().getMessage()));
                stackUpdater.updateStackStatus(context.getStack().getId(), VERTICAL_SCALE_FAILED, errorMessage);
                if (isOperationIdSet(variables)) {
                    operationService.failOperation(context.getStack().getAccountId(), getOperationId(variables), errorMessage);
                }
                enableStatusChecker(context.getStack(), "Failed rolling vertical scale");
                sendEvent(context, ROLLING_VERTICAL_SCALE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private CloudInstance getCloudInstance(StackContext context, String instanceId) {
        InstanceMetaData instance = instanceMetaDataService.getByInstanceIds(context.getStack().getId(), List.of(instanceId))
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Instance not found: " + instanceId));
        return instanceMetaDataToCloudInstanceConverter.convert(instance);
    }

}
