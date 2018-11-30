package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
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
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
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
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.MountDisksOnNewHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.MountDisksOnNewHostsResult;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackService stackService;

    @Bean(name = "UPSCALE_PREVALIDATION_STATE")
    public Action<?, ?> prevalidate() {
        return new AbstractStackUpscaleAction<>(StackScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(INSTANCEGROUPNAME, payload.getInstanceGroup());
                variables.put(ADJUSTMENT, payload.getAdjustment());
                variables.put(HOSTNAMES, payload.getHostNames());
            }

            @Override
            protected void doExecute(StackScalingFlowContext context, StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                int instanceCountToCreate = getInstanceCountToCreate(context.getStack(), payload.getInstanceGroup(), payload.getAdjustment());
                stackUpscaleService.addInstanceFireEventAndLog(context.getStack(), payload.getAdjustment());
                if (instanceCountToCreate > 0) {
                    stackUpscaleService.startAddInstances(context.getStack(), payload.getAdjustment());
                    sendEvent(context);
                } else {
                    List<CloudResourceStatus> list = resourceService.getAllAsCloudResourceStatus(payload.getStackId());
                    CloudStack stack = cloudStackConverter.convert(stackService.getByIdWithListsInTransaction(payload.getStackId()));
                    UpscaleStackRequest<UpscaleStackResult> request =
                            new UpscaleStackRequest<>(context.getCloudContext(), context.getCloudCredential(), stack, ResourceLists.transform(list));
                    UpscaleStackResult result = new UpscaleStackResult(request, ResourceStatus.CREATED, list);
                    sendEvent(context.getFlowId(), result.selector(), result);
                }
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                LOGGER.debug("Assembling upscale stack event for stack: {}", context.getStack());
                List<CloudInstance> newInstances = stackUpscaleService.buildNewInstances(context.getStack(), context.getInstanceGroupName(),
                        getInstanceCountToCreate(context.getStack(), context.getInstanceGroupName(), context.getAdjustment()));
                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), newInstances, false);
                CloudStack cloudStack = cloudStackConverter.convert(updatedStack);
                return new UpscaleStackValidationRequest<UpscaleStackValidationResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "ADD_INSTANCES_STATE")
    public Action<?, ?> addInstances() {
        return new AbstractStackUpscaleAction<>(UpscaleStackValidationResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleStackValidationResult payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                LOGGER.debug("Assembling upscale stack event for stack: {}", context.getStack());
                List<CloudInstance> newInstances = stackUpscaleService.buildNewInstances(context.getStack(), context.getInstanceGroupName(),
                        getInstanceCountToCreate(context.getStack(), context.getInstanceGroupName(), context.getAdjustment()));
                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), newInstances, true);
                List<CloudResource> resources = cloudResourceConverter.convert(context.getStack().getResources());
                CloudStack updatedCloudStack = cloudStackConverter.convert(updatedStack);
                return new UpscaleStackRequest<UpscaleStackResult>(context.getCloudContext(), context.getCloudCredential(), updatedCloudStack, resources);
            }
        };
    }

    private int getInstanceCountToCreate(Stack stack, String instanceGroupName, int adjusment) {
        Set<InstanceMetaData> instanceMetadata = instanceMetaDataService.unusedInstancesInInstanceGroupByName(stack.getId(), instanceGroupName);
        return adjusment - instanceMetadata.size();
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
                Set<String> instanceMetaData = instanceMetaDataService.unusedInstancesInInstanceGroupByName(context.getStack().getId(),
                        context.getInstanceGroupName()).stream()
                        .map(InstanceMetaData::getInstanceId)
                        .collect(Collectors.toSet());
                List<CloudInstance> newCloudInstances = allKnownInstances.stream()
                        .filter(cloudInstance -> InstanceStatus.CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus())
                                || instanceMetaData.contains(cloudInstance.getInstanceId()))
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
                Set<String> upscaleCandidateAddresses = stackUpscaleService.finishExtendMetadata(context.getStack(), context.getInstanceGroupName(), payload);
                variables.put(UPSCALE_CANDIDATE_ADDRESSES, upscaleCandidateAddresses);
                InstanceGroup ig = instanceGroupRepository.findOneByGroupNameInStack(payload.getStackId(), context.getInstanceGroupName());
                if (InstanceGroupType.GATEWAY == ig.getInstanceGroupType()) {
                    Stack stack = stackService.getByIdWithListsInTransaction(context.getStack().getId());
                    InstanceMetaData gatewayMetaData = stack.getPrimaryGatewayInstance();
                    CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData);
                    Selectable sshFingerPrintReq = new GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.getCloudContext(),
                            context.getCloudCredential(), gatewayInstance);
                    sendEvent(context.getFlowId(), sshFingerPrintReq);
                } else {
                    BootstrapNewNodesEvent bootstrapPayload = new BootstrapNewNodesEvent(context.getStack().getId(), upscaleCandidateAddresses);
                    sendEvent(context.getFlowId(), StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT.event(), bootstrapPayload);
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
                BootstrapNewNodesEvent bootstrapNewNodesEvent =
                        new BootstrapNewNodesEvent(payload.getStackId(), (Set<String>) variables.get(UPSCALE_CANDIDATE_ADDRESSES));
                sendEvent(context.getFlowId(), StackCreationEvent.TLS_SETUP_FINISHED_EVENT.event(), bootstrapNewNodesEvent);
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
                        payload.getUpscaleCandidateAddresses(), context.getHostNames());
                sendEvent(context.getFlowId(), request);
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
                sendEvent(context.getFlowId(), request);
            }
        };
    }

    @Bean(name = "EXTEND_HOST_METADATA_FINISHED_STATE")
    public Action<?, ?> finishExtendHostMetadata() {
        return new AbstractStackUpscaleAction<>(ExtendHostMetadataResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, ExtendHostMetadataResult payload, Map<Object, Object> variables) {
                Selectable request = new MountDisksOnNewHostsRequest(context.getStack().getId(),
                        payload.getRequest().getUpscaleCandidateAddresses());
                sendEvent(context.getFlowId(), request);
            }
        };
    }

    @Bean(name = "MOUNT_DISKS_ON_NEW_HOSTS_STATE")
    public Action<?, ?> mountDisksOnNewHosts() {
        return new AbstractStackUpscaleAction<>(MountDisksOnNewHostsResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, MountDisksOnNewHostsResult payload, Map<Object, Object> variables) {
                stackUpscaleService.mountDisksOnNewHosts(context.getStack());
                metricService.incrementMetricCounter(MetricType.STACK_UPSCALE_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPSCALE_FAILED_STATE")
    public Action<?, ?> stackStartFailedAction() {
        return new AbstractStackFailureAction<StackUpscaleState, StackUpscaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackUpscaleService.handleStackUpscaleFailure(context.getStackView().getId(), payload);
                metricService.incrementMetricCounter(MetricType.STACK_UPSCALE_FAILED, context.getStackView());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
