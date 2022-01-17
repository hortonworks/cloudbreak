package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.HOST_GROUP_WITH_ADJUSTMENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.HOST_GROUP_WITH_HOSTNAMES;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Configuration
public class StackUpscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleActions.class);

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Bean(name = "UPSCALE_PREVALIDATION_STATE")
    public Action<?, ?> prevalidate() {
        return new AbstractStackUpscaleAction<>(StackScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOST_GROUP_WITH_ADJUSTMENT, payload.getHostGroupsWithAdjustment());
                variables.put(HOST_GROUP_WITH_HOSTNAMES, payload.getHostGroupsWithHostNames());
                variables.put(REPAIR, payload.isRepair());
                if (payload.getTriggeredStackVariant() != null) {
                    variables.put(TRIGGERED_VARIANT, payload.getTriggeredStackVariant());
                }
                variables.put(NETWORK_SCALE_DETAILS, payload.getNetworkScaleDetails());
                variables.put(ADJUSTMENT_WITH_THRESHOLD, payload.getAdjustmentTypeWithThreshold());
            }

            @Override
            protected void doExecute(StackScalingFlowContext context, StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                Map<String, Integer> hostGroupsWithAdjustment = payload.getHostGroupsWithAdjustment();
                int instanceCountToCreate = 0;
                for (Map.Entry<String, Integer> hostGroupWithAdjustment : hostGroupsWithAdjustment.entrySet()) {
                    instanceCountToCreate += stackUpscaleService.getInstanceCountToCreate(context.getStack(), hostGroupWithAdjustment.getKey(),
                            hostGroupWithAdjustment.getValue(), context.isRepair());
                }

                stackUpscaleService.addInstanceFireEventAndLog(context.getStack(), hostGroupsWithAdjustment, payload.getAdjustmentTypeWithThreshold());
                if (instanceCountToCreate > 0) {
                    stackUpscaleService.startAddInstances(context.getStack(), hostGroupsWithAdjustment);
                    sendEvent(context);
                } else {
                    List<CloudResourceStatus> list = resourceService.getAllAsCloudResourceStatus(payload.getResourceId());
                    UpscaleStackResult result = new UpscaleStackResult(payload.getResourceId(), ResourceStatus.CREATED, list);
                    sendEvent(context, result.selector(), result);
                }
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                Map<String, Integer> hostGroupWithInstanceCountToCreate = getHostGroupsWithInstanceCountToCreate(context);
                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), hostGroupWithInstanceCountToCreate,
                        context.getHostgroupWithHostnames(), false, context.isRepair(), context.getStackNetworkScaleDetails());
                CloudStack cloudStack = cloudStackConverter.convert(updatedStack);
                return new UpscaleStackValidationRequest<UpscaleStackValidationResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "ADD_INSTANCES_STATE")
    public Action<?, ?> addInstances() {
        return new AbstractStackUpscaleAction<>(UpscaleStackValidationResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleStackValidationResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                Map<String, Integer> hostGroupWithInstanceCountToCreate = getHostGroupsWithInstanceCountToCreate(context);

                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), hostGroupWithInstanceCountToCreate,
                        context.getHostgroupWithHostnames(), true, context.isRepair(), context.getStackNetworkScaleDetails());
                List<CloudResource> resources = context.getStack().getResources().stream()
                        .map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                CloudStack updatedCloudStack = cloudStackConverter.convert(updatedStack);
                AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = context.getAdjustmentTypeWithThreshold();
                if (adjustmentTypeWithThreshold == null) {
                    Integer exactNumber = hostGroupWithInstanceCountToCreate.values().stream().reduce(0, Integer::sum);
                    adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, exactNumber.longValue());
                }
                LOGGER.info("Adjustment type with threshold for upscale request: {}", adjustmentTypeWithThreshold);
                return new UpscaleStackRequest<UpscaleStackResult>(context.getCloudContext(), context.getCloudCredential(), updatedCloudStack, resources,
                        adjustmentTypeWithThreshold);
            }
        };
    }

    @Bean(name = "ADD_INSTANCES_FINISHED_STATE")
    public Action<?, ?> finishAddInstances() {
        return new AbstractStackUpscaleAction<>(UpscaleStackResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleStackResult payload, Map<Object, Object> variables) {
                stackUpscaleService.finishAddInstances(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackUpscaleEvent.EXTEND_METADATA_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_STATE")
    public Action<?, ?> extendMetadata() {
        return new AbstractStackUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpscaleService.extendingMetadata(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                List<CloudResource> cloudResources = context.getStack().getResources().stream()
                        .map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                List<CloudInstance> allKnownInstances = cloudStackConverter.buildInstances(context.getStack());
                LOGGER.info("All known instances: {}", allKnownInstances);
                Set<String> unusedInstancesForGroup = new HashSet<>();
                for (String hostGroup : context.getHostGroupWithAdjustment().keySet()) {
                    unusedInstancesForGroup.addAll(instanceMetaDataService.unusedInstancesInInstanceGroupByName(context.getStack().getId(),
                            hostGroup).stream()
                            .map(InstanceMetaData::getInstanceId)
                            .collect(Collectors.toSet()));
                }

                LOGGER.info("Unused instances for group: {}", unusedInstancesForGroup);
                List<CloudInstance> newCloudInstances = allKnownInstances.stream()
                        .filter(cloudInstance -> InstanceStatus.CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus())
                                || unusedInstancesForGroup.contains(cloudInstance.getInstanceId()))
                        .collect(Collectors.toList());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, newCloudInstances,
                        allKnownInstances);
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_FINISHED_STATE")
    public Action<?, ?> finishExtendMetadata() {
        return new AbstractStackUpscaleAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, CollectMetadataResult payload, Map<Object, Object> variables)
                    throws TransactionExecutionException {
                Integer adjustment = context.getHostGroupWithAdjustment().values().stream().reduce(0, Integer::sum);
                Set<String> upscaleCandidateAddresses = stackUpscaleService.finishExtendMetadata(context.getStack(), adjustment, payload);
                variables.put(UPSCALE_CANDIDATE_ADDRESSES, upscaleCandidateAddresses);
                Set<String> hostGroups = context.getHostGroupWithAdjustment().keySet();
                List<InstanceGroup> scaledInstanceGroups = instanceGroupService.findByStackIdAndInstanceGroupNames(payload.getResourceId(),
                        hostGroups);
                boolean gatewayWasUpscaled = scaledInstanceGroups.stream()
                        .anyMatch(instanceGroup -> InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType()));
                if (gatewayWasUpscaled) {
                    LOGGER.info("Gateway type instance group");
                    Stack stack = stackService.getByIdWithListsInTransaction(context.getStack().getId());
                    InstanceMetaData gatewayMetaData = stack.getPrimaryGatewayInstance();
                    if (null == gatewayMetaData) {
                        throw new CloudbreakServiceException("Could not get gateway instance metadata from the cloud provider.");
                    }
                    DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(stack.getEnvironmentCrn());
                    CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData, environment, stack.getStackAuthentication());
                    LOGGER.info("Send GetSSHFingerprintsRequest because we need to collect SSH fingerprints");
                    Selectable sshFingerPrintReq = new GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.getCloudContext(),
                            context.getCloudCredential(), gatewayInstance);
                    sendEvent(context, sshFingerPrintReq);
                } else {
                    BootstrapNewNodesEvent bootstrapPayload = new BootstrapNewNodesEvent(context.getStack().getId());
                    sendEvent(context, StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT.event(), bootstrapPayload);
                }
            }
        };
    }

    @Bean(name = "GATEWAY_TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractStackUpscaleAction<>(GetSSHFingerprintsResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, GetSSHFingerprintsResult payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.setupTls(context);
                StackEvent event =
                        new StackEvent(payload.getResourceId());
                sendEvent(context, StackCreationEvent.TLS_SETUP_FINISHED_EVENT.event(), event);
            }
        };
    }

    @Bean(name = "RE_REGISTER_WITH_CLUSTER_PROXY_STATE")
    public Action<?, ?> reRegisterWithClusterProxy() {
        return new AbstractStackUpscaleAction<>(StackEvent.class) {
            @Inject
            private ClusterProxyEnablementService clusterProxyEnablementService;

            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) {
                if (clusterProxyEnablementService.isClusterProxyApplicable(context.getStack().cloudPlatform())) {
                    stackUpscaleService.reRegisterWithClusterProxy(context.getStack().getId());
                    sendEvent(context);
                } else {
                    LOGGER.info("Cluster Proxy integration is DISABLED, skipping re-registering with Cluster Proxy service");
                    BootstrapNewNodesEvent bootstrapNewNodesEvent =
                            new BootstrapNewNodesEvent(StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT.event(), payload.getResourceId());
                    sendEvent(context, bootstrapNewNodesEvent);
                }
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new ClusterProxyReRegistrationRequest(context.getStack().getId(),
                        context.getHostGroupWithAdjustment().keySet(),
                        context.getStack().getCloudPlatform());
            }
        };
    }

    @Bean(name = "BOOTSTRAP_NEW_NODES_STATE")
    public Action<?, ?> bootstrapNewNodes() {
        return new AbstractStackUpscaleAction<>(BootstrapNewNodesEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, BootstrapNewNodesEvent payload, Map<Object, Object> variables) {
                stackUpscaleService.bootstrappingNewNodes(context.getStack());
                Map<String, Set<String>> hostgroupWithHostnames = context.getHostgroupWithHostnames();
                Selectable request = new BootstrapNewNodesRequest(context.getStack().getId(),
                        (Set<String>) variables.get(UPSCALE_CANDIDATE_ADDRESSES),
                        hostgroupWithHostnames.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "EXTEND_HOST_METADATA_STATE")
    public Action<?, ?> extendHostMetadata() {
        return new AbstractStackUpscaleAction<>(BootstrapNewNodesResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, BootstrapNewNodesResult payload, Map<Object, Object> variables) {
                stackUpscaleService.extendingHostMetadata(context.getStack());
                Selectable request = new ExtendHostMetadataRequest(context.getStack().getId(),
                        payload.getRequest().getUpscaleCandidateAddresses());
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "CLEANUP_FREEIPA_UPSCALE_STATE")
    public Action<?, ?> cleanupFreeIpaAction() {
        return new AbstractStackUpscaleAction<>(ExtendHostMetadataResult.class) {

            @Inject
            private InstanceMetaDataService instanceMetaDataService;

            @Override
            protected void doExecute(StackScalingFlowContext context, ExtendHostMetadataResult payload, Map<Object, Object> variables) {
                Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.findNotTerminatedAndNotZombieForStack(context.getStack().getId());
                Set<String> ips = payload.getRequest().getUpscaleCandidateAddresses();
                Set<String> hostNames = instanceMetaData.stream()
                        .filter(im -> ips.contains(im.getPrivateIp()))
                        .filter(im -> im.getDiscoveryFQDN() != null)
                        .map(InstanceMetaData::getDiscoveryFQDN)
                        .collect(Collectors.toSet());
                CleanupFreeIpaEvent cleanupFreeIpaEvent = new CleanupFreeIpaEvent(context.getStack().getId(), hostNames, ips, context.isRepair());
                sendEvent(context, cleanupFreeIpaEvent);
            }
        };
    }

    @Bean(name = "EXTEND_HOST_METADATA_FINISHED_STATE")
    public Action<?, ?> finishExtendHostMetadata() {
        return new AbstractStackUpscaleAction<>(CleanupFreeIpaEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, CleanupFreeIpaEvent payload, Map<Object, Object> variables) {
                final Stack stack = context.getStack();
                stackUpscaleService.finishExtendHostMetadata(stack);
                final Set<String> newAddresses = payload.getIps();
                final Map<String, String> newAddressesByFqdn = stack.getNotDeletedAndNotZombieInstanceMetaDataSet().stream()
                        .filter(instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()))
                        .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                        .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getPublicIpWrapper));
                clusterPublicEndpointManagementService.upscale(stack, newAddressesByFqdn);
                getMetricService().incrementMetricCounter(MetricType.STACK_UPSCALE_SUCCESSFUL, stack);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPSCALE_FAILED_STATE")
    public Action<?, ?> stackUpscaleFailedAction() {
        return new AbstractStackFailureAction<StackUpscaleState, StackUpscaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                Map<String, Set<String>> hostgroupWithHostnames = (Map<String, Set<String>>)  variables.getOrDefault(HOST_GROUP_WITH_HOSTNAMES,
                        new HashMap<>());
                Map<String, Integer> hostGroupWithAdjustment = (Map<String, Integer>) variables.getOrDefault(HOST_GROUP_WITH_ADJUSTMENT, new HashMap<>());
                stackUpscaleService.handleStackUpscaleFailure(isRepair(variables), hostgroupWithHostnames, payload.getException(),
                        payload.getResourceId(), hostGroupWithAdjustment);
                getMetricService().incrementMetricCounter(MetricType.STACK_UPSCALE_FAILED, context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

    private Map<String, Integer> getHostGroupsWithInstanceCountToCreate(StackScalingFlowContext context) {
        LOGGER.debug("Assembling upscale stack event for stack: {}", context.getStack());
        Map<String, Integer> hostGroupsWithAdjustment = context.getHostGroupWithAdjustment();
        Map<String, Integer> hostGroupWithInstanceCountToCreate = new HashMap<>();
        for (Map.Entry<String, Integer> hostGroupWithAdjustment : hostGroupsWithAdjustment.entrySet()) {
            String hostGroup = hostGroupWithAdjustment.getKey();
            int instanceCountToCreate = stackUpscaleService.getInstanceCountToCreate(context.getStack(), hostGroup,
                    hostGroupWithAdjustment.getValue(), context.isRepair());
            hostGroupWithInstanceCountToCreate.put(hostGroup, instanceCountToCreate);
        }
        return hostGroupWithInstanceCountToCreate;
    }

    private boolean isRepair(Map<Object, Object> variables) {
        return (boolean) variables.getOrDefault(AbstractStackUpscaleAction.REPAIR, Boolean.FALSE);
    }
}
