package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.HOSTNAMES;

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
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
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
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;

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
    private StackScalabilityCondition stackScalabilityCondition;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Bean(name = "UPSCALE_PREVALIDATION_STATE")
    public Action<?, ?> prevalidate() {
        return new AbstractStackUpscaleAction<>(StackScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(INSTANCEGROUPNAME, payload.getInstanceGroup());
                variables.put(ADJUSTMENT, payload.getAdjustment());
                variables.put(HOSTNAMES, payload.getHostNames());
                variables.put(REPAIR, payload.isRepair());
            }

            @Override
            protected void doExecute(StackScalingFlowContext context, StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                int instanceCountToCreate = getInstanceCountToCreate(context.getStack(), payload.getInstanceGroup(), payload.getAdjustment());
                stackUpscaleService.addInstanceFireEventAndLog(context.getStack(), payload.getAdjustment(), payload.getInstanceGroup());
                if (instanceCountToCreate > 0) {
                    stackUpscaleService.startAddInstances(context.getStack(), payload.getAdjustment(), payload.getInstanceGroup());
                    sendEvent(context);
                } else {
                    List<CloudResourceStatus> list = resourceService.getAllAsCloudResourceStatus(payload.getResourceId());
                    UpscaleStackResult result = new UpscaleStackResult(payload.getResourceId(), ResourceStatus.CREATED, list);
                    sendEvent(context, result.selector(), result);
                }
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                LOGGER.debug("Assembling upscale stack event for stack: {}", context.getStack());
                List<CloudInstance> newInstances = stackUpscaleService.buildNewInstances(context.getStack(), context.getInstanceGroupName(),
                        getInstanceCountToCreate(context.getStack(), context.getInstanceGroupName(), context.getAdjustment()));
                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), newInstances, false, context.getHostNames(),
                        context.isRepair());
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
                LOGGER.debug("Assembling upscale stack event for stack: {}", context.getStack());
                List<CloudInstance> newInstances = stackUpscaleService.buildNewInstances(context.getStack(), context.getInstanceGroupName(),
                        getInstanceCountToCreate(context.getStack(), context.getInstanceGroupName(), context.getAdjustment()));
                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), newInstances, true, context.getHostNames(),
                        context.isRepair());
                List<CloudResource> resources = cloudResourceConverter.convert(context.getStack().getResources());
                CloudStack updatedCloudStack = cloudStackConverter.convert(updatedStack);
                return new UpscaleStackRequest<UpscaleStackResult>(context.getCloudContext(), context.getCloudCredential(), updatedCloudStack, resources);
            }
        };
    }

    private int getInstanceCountToCreate(Stack stack, String instanceGroupName, int adjustment) {
        Set<InstanceMetaData> instanceMetadata = instanceMetaDataService.unusedInstancesInInstanceGroupByName(stack.getId(), instanceGroupName);
        return stackScalabilityCondition.isScalable(stack, instanceGroupName) ? adjustment - instanceMetadata.size() : 0;
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
                List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
                List<CloudInstance> allKnownInstances = cloudStackConverter.buildInstances(context.getStack());
                LOGGER.info("All known instances: {}", allKnownInstances);
                Set<String> unusedInstancesForGroup = instanceMetaDataService.unusedInstancesInInstanceGroupByName(context.getStack().getId(),
                        context.getInstanceGroupName()).stream()
                        .map(InstanceMetaData::getInstanceId)
                        .collect(Collectors.toSet());
                LOGGER.info("Unused instances for group: {}", unusedInstancesForGroup);
                List<CloudInstance> newCloudInstances = allKnownInstances.stream()
                        .filter(cloudInstance -> InstanceStatus.CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus())
                                || unusedInstancesForGroup.contains(cloudInstance.getInstanceId()))
                        .collect(Collectors.toList());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, newCloudInstances, allKnownInstances);
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_FINISHED_STATE")
    public Action<?, ?> finishExtendMetadata() {
        return new AbstractStackUpscaleAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, CollectMetadataResult payload, Map<Object, Object> variables)
                    throws TransactionExecutionException {
                Set<String> upscaleCandidateAddresses = stackUpscaleService.finishExtendMetadata(context.getStack(), context.getAdjustment(), payload);
                variables.put(UPSCALE_CANDIDATE_ADDRESSES, upscaleCandidateAddresses);
                InstanceGroup ig = instanceGroupService.findOneWithInstanceMetadataByGroupNameInStack(payload.getResourceId(), context.getInstanceGroupName())
                        .orElseThrow(NotFoundException.notFound("instanceGroup", context.getInstanceGroupName()));
                if (InstanceGroupType.GATEWAY == ig.getInstanceGroupType()) {
                    LOGGER.info("Gateway type instance group");
                    Stack stack = stackService.getByIdWithListsInTransaction(context.getStack().getId());
                    InstanceMetaData gatewayMetaData = stack.getPrimaryGatewayInstance();
                    if (null == gatewayMetaData) {
                        throw new CloudbreakServiceException("Could not get gateway instance metadata from the cloud provider.");
                    }
                    CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData, stack.getEnvironmentCrn(), stack.getStackAuthentication());
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
                        context.getInstanceGroupName(),
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
                Selectable request = new BootstrapNewNodesRequest(context.getStack().getId(),
                        (Set<String>) variables.get(UPSCALE_CANDIDATE_ADDRESSES), context.getHostNames());
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
                Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.findNotTerminatedForStack(context.getStack().getId());
                Set<String> ips = payload.getRequest().getUpscaleCandidateAddresses();
                Set<String> hostNames = instanceMetaData.stream()
                        .filter(im -> ips.contains(im.getPrivateIp()))
                        .map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet());
                CleanupFreeIpaEvent cleanupFreeIpaEvent = new CleanupFreeIpaEvent(context.getStack().getId(), hostNames, ips, isRepair(variables));
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
                final Map<String, String> newAddressesByFqdn = stack.getNotDeletedInstanceMetaDataSet().stream()
                        .filter(instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()))
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
                Set<String> hostNames = (Set<String>) variables.getOrDefault(HOSTNAMES, new HashSet<>());
                stackUpscaleService.handleStackUpscaleFailure(isRepair(variables), hostNames, payload.getException(),
                        payload.getResourceId());
                getMetricService().incrementMetricCounter(MetricType.STACK_UPSCALE_FAILED, context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

    private boolean isRepair(Map<Object, Object> variables) {
        return (boolean) variables.getOrDefault(AbstractStackUpscaleAction.REPAIR, Boolean.FALSE);
    }
}
