package com.sequenceiq.freeipa.flow.freeipa.upscale.action;

import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_DISABLE_STATUS_CHECKER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_DISABLE_STATUS_CHECKER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_ENABLE_STATUS_CHECKER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_ENABLE_STATUS_CHECKER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_SAVE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_STARTING_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.common.api.type.CommonResourceType;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.BootstrapMachinesFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.CollectMetadataResultToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.HostMetadataSetupFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.InstallFreeIpaServicesFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.PostInstallFreeIpaFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.UpscaleStackResultToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.TlsSetupService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.instance.MetadataSetupService;

@Configuration
public class FreeIpaUpscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpscaleActions.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter resourceConverter;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private PrivateIdProvider privateIdProvider;

    @Bean(name = "UPSCALE_STARTING_STATE")
    public Action<?, ?> startingAction() {
        return new AbstractUpscaleAction<>(UpscaleEvent.class) {
            @Override
            protected void doExecute(StackContext context, UpscaleEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String operationId = payload.getOperationId();
                setOperationId(variables, operationId);
                setInstanceCountByGroup(variables, payload.getInstanceCountByGroup());
                setRepair(variables, payload.isRepair());
                LOGGER.info("Starting upscale {}", payload);
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Starting upscale");
                sendEvent(context, UPSCALE_STARTING_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_DISABLE_STATUS_CHECKER_STATE")
    public Action<?, ?> disableStatusCheckerAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Disabling the status checker while upscaling");
                try {
                    if (!isRepair(variables)) {
                        disableStatusChecker(stack, "Upscaling FreeIPA");
                    }
                    sendEvent(context, UPSCALE_DISABLE_STATUS_CHECKER_FINISHED_EVENT.event(), new StackEvent(stack.getId()));
                } catch (Exception e) {
                    LOGGER.error("Failed to disable the status checker", e);
                    sendEvent(context, UPSCALE_DISABLE_STATUS_CHECKER_FAILED_EVENT.event(),
                            new UpscaleFailureEvent(stack.getId(), "disable status checker", Set.of(), Map.of(), e));
                }
            }
        };
    }

    @Bean(name = "UPSCALE_ADD_INSTANCES_STATE")
    public Action<?, ?> addInstancesAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Adding instances");

                List<CloudInstance> newInstances = buildNewInstances(context.getStack(), getInstanceCountByGroup(variables));
                if (newInstances.isEmpty()) {
                    skipAddingNewInstances(context, stack);
                } else {
                    addNewInstances(context, stack, newInstances);
                }
            }

            private void skipAddingNewInstances(StackContext context, Stack stack) {
                List<CloudResourceStatus> list = resourceService.getAllAsCloudResourceStatus(stack.getId());
                UpscaleStackRequest<UpscaleStackResult> request = new UpscaleStackRequest<>(
                        context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(), ResourceLists.transform(list));
                UpscaleStackResult result = new UpscaleStackResult(request.getResourceId(), ResourceStatus.CREATED, list);
                sendEvent(context, result.selector(), result);
            }

            private void addNewInstances(StackContext context, Stack stack, List<CloudInstance> newInstances) {
                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, newInstances);
                List<CloudResource> cloudResources = resourceService.findAllByStackId(stack.getId()).stream()
                        .map(resource -> resourceConverter.convert(resource))
                        .collect(Collectors.toList());
                CloudStack updatedCloudStack = cloudStackConverter.convert(updatedStack);
                UpscaleStackRequest<UpscaleStackResult> request = new UpscaleStackRequest<>(
                        context.getCloudContext(), context.getCloudCredential(), updatedCloudStack, cloudResources);
                sendEvent(context, request.selector(), request);
            }

            private List<CloudInstance> buildNewInstances(Stack stack, int instanceCountByGroup) {
                long privateId = privateIdProvider.getFirstValidPrivateId(stack.getInstanceGroups());
                List<CloudInstance> newInstances = new ArrayList<>();
                for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                    int remainingInstances = instanceCountByGroup - instanceGroup.getNotDeletedInstanceMetaDataSet().size();
                    for (long i = 0; i < remainingInstances; ++i) {
                        newInstances.add(cloudStackConverter.buildInstance(stack, null, instanceGroup,
                                stack.getStackAuthentication(), privateId++, InstanceStatus.CREATE_REQUESTED));
                    }
                }
                return newInstances;
            }

        };
    }

    @Bean(name = "UPSCALE_VALIDATE_INSTANCES_STATE")
    public Action<?, ?> validateInstancesAction() {
        return new AbstractUpscaleAction<>(UpscaleStackResult.class) {
            @Override
            protected void doExecute(StackContext context, UpscaleStackResult payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Validating new instances");
                try {
                    finishAddInstances(context, payload);
                    sendEvent(context, UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
                } catch (Exception e) {
                    sendEvent(context, UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT.selector(),
                            new UpscaleFailureEvent(stack.getId(), "Validating new instances", Set.of(), Map.of(), e));
                }
            }

            private void finishAddInstances(StackContext context, UpscaleStackResult payload) {
                LOGGER.debug("Upscale stack result: {}", payload);
                List<CloudResourceStatus> results = payload.getResults();
                validateResourceResults(context, payload.getErrorDetails(), results);
                Set<Resource> resourceSet = transformResults(results, context.getStack());
                if (resourceSet.isEmpty()) {
                    metadataSetupService.cleanupRequestedInstances(context.getStack());
                    throw new OperationException("Failed to upscale the cluster since all create request failed. Resource set is empty");
                }
                LOGGER.debug("Adding new instances to the stack is DONE");
            }

            private void validateResourceResults(StackContext context, Exception exception, List<CloudResourceStatus> results) {
                if (exception != null) {
                    LOGGER.info(format("Failed to upscale stack: %s", context.getCloudContext()), exception);
                    throw new OperationException(exception);
                }
                List<CloudResourceStatus> templates = results.stream().filter(result -> CommonResourceType.TEMPLATE == result.getCloudResource().getType()
                        .getCommonResourceType()).collect(Collectors.toList());
                if (!templates.isEmpty() && (templates.get(0).isFailed() || templates.get(0).isDeleted())) {
                    throw new OperationException(format("Failed to upscale the stack for %s due to: %s",
                            context.getCloudContext(), templates.get(0).getStatusReason()));
                }
            }

            private Set<Resource> transformResults(Iterable<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
                Set<Resource> retSet = new HashSet<>();
                for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
                    if (!cloudResourceStatus.isFailed()) {
                        CloudResource cloudResource = cloudResourceStatus.getCloudResource();
                        Resource resource = new Resource(
                                cloudResource.getType(), cloudResource.getName(), cloudResource.getReference(), cloudResource.getStatus(), stack, null);
                        retSet.add(resource);
                    }
                }
                return retSet;
            }
        };
    }

    @Bean(name = "UPSCALE_EXTEND_METADATA_STATE")
    public Action<?, ?> extendMetadataAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {

            private final Set<InstanceStatus> unusedInstanceStatuses = Set.of(InstanceStatus.CREATE_REQUESTED, InstanceStatus.CREATED);

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Extending metadata");

                List<CloudInstance> allKnownInstances = cloudStackConverter.buildInstances(stack);
                List<Resource> resources = resourceService.findAllByStackId(stack.getId());
                List<CloudResource> cloudResources = resourceConverter.convert(resources);
                List<CloudInstance> newCloudInstances = allKnownInstances.stream()
                        .filter(this::isNewInstances)
                        .collect(Collectors.toList());
                CollectMetadataRequest request = new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources,
                        newCloudInstances, allKnownInstances);

                sendEvent(context, request.selector(), request);
            }

            private boolean isNewInstances(CloudInstance cloudInstance) {
                return unusedInstanceStatuses.contains(cloudInstance.getTemplate().getStatus());
            }
        };
    }

    @Bean(name = "UPSCALE_SAVE_METADATA_STATE")
    public Action<?, ?> saveMetadataAction() {
        return new AbstractUpscaleAction<>(CollectMetadataResult.class) {

            @Override
            protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Saving metadata");
                List<String> instanceIds = payload.getResults().stream()
                        .map(CloudVmMetaDataStatus::getCloudVmInstanceStatus)
                        .map(CloudVmInstanceStatus::getCloudInstance)
                        .map(CloudInstance::getInstanceId)
                        .collect(Collectors.toList());
                setInstanceIds(variables, instanceIds);
                metadataSetupService.saveInstanceMetaData(stack, payload.getResults(),
                        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED);
                sendEvent(context, UPSCALE_SAVE_METADATA_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Setting up TLS");

                StackEvent event;
                try {
                    if (!stack.getTunnel().useCcm()) {
                        Set<InstanceMetaData> newInstancesMetaData = stack.getInstanceGroups().stream()
                                .flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream())
                                .filter(this::isNewInstancesWithoutTlsCert)
                                .collect(Collectors.toSet());
                        for (InstanceMetaData gwInstance : newInstancesMetaData) {
                            tlsSetupService.setupTls(stack.getId(), gwInstance);
                        }
                    }
                    event = new StackEvent(UpscaleFlowEvent.UPSCALE_TLS_SETUP_FINISHED_EVENT.event(), stack.getId());
                } catch (Exception e) {
                    event = new UpscaleFailureEvent(stack.getId(), "Setting ups TLS", Set.of(), Map.of(), e);
                }
                sendEvent(context, event.selector(), event);
            }

            private boolean isNewInstancesWithoutTlsCert(InstanceMetaData instanceMetaData) {
                return instanceMetaData.getInstanceStatus().equals(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED) &&
                        Objects.isNull(instanceMetaData.getServerCert());
            }

        };
    }

    @Bean(name = "UPSCALE_BOOTSTRAPPING_MACHINES_STATE")
    public Action<?, ?> bootstrappingMachinesAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Bootstrapping machines");
                BootstrapMachinesRequest request = new BootstrapMachinesRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_COLLECTING_HOST_METADATA_STATE")
    public Action<?, ?> collectingHostMetadataAction() {
        return new AbstractUpscaleAction<>(BootstrapMachinesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Collecting host metadata");
                HostMetadataSetupRequest request = new HostMetadataSetupRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_RECORD_HOSTNAMES_STATE")
    public Action<?, ?> recordHostnamesAction() {
        return new AbstractUpscaleAction<>(HostMetadataSetupSuccess.class) {
            @Override
            protected void doExecute(StackContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Recording hostnames");
                List<String> instanceIds = getInstanceIds(variables);
                List<String> hosts = stack.getNotDeletedInstanceMetaDataList().stream()
                        .filter(instanceMetaData -> instanceIds.contains(instanceMetaData.getInstanceId()))
                        .map(InstanceMetaData::getDiscoveryFQDN)
                        .collect(Collectors.toList());
                setUpscaleHosts(variables, hosts);
                sendEvent(context, UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_FREEIPA_INSTALL_STATE")
    public Action<?, ?> installFreeIpaAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Installing FreeIPA");
                InstallFreeIpaServicesRequest request = new InstallFreeIpaServicesRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_CLUSTERPROXY_REGISTRATION_STATE")
    public Action<?, ?> updateClusterProxyRegistrationAction() {
        return new AbstractUpscaleAction<>(InstallFreeIpaServicesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, InstallFreeIpaServicesSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Update cluster proxy registration");
                ClusterProxyUpdateRegistrationRequest request = new ClusterProxyUpdateRegistrationRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_FREEIPA_POST_INSTALL_STATE")
    public Action<?, ?> freeIpaPostInstallAction() {
        return new AbstractUpscaleAction<>(ClusterProxyUpdateRegistrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, ClusterProxyUpdateRegistrationSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "FreeIPA Post Installation");
                PostInstallFreeIpaRequest request = new PostInstallFreeIpaRequest(stack.getId(), false);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_METADATA_STATE")
    public Action<?, ?> updateMetadataAction() {
        return new AbstractUpscaleAction<>(PostInstallFreeIpaSuccess.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(StackContext context, PostInstallFreeIpaSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Upscale update metadata");
                if (!isRepair(variables)) {
                    int nodeCount = getInstanceCountByGroup(variables);
                    for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                        instanceGroup.setNodeCount(nodeCount);
                        instanceGroupService.save(instanceGroup);
                    }
                }
                sendEvent(context, UPSCALE_UPDATE_METADATA_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_ENABLE_STATUS_CHECKER_STATE")
    public Action<?, ?> enableStatusCheckerAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getInProgressStatus(variables), "Enabling the status checker after upscaling");
                try {
                    if (!isRepair(variables)) {
                        enableStatusChecker(stack, "Finished upscaling FreeIPA");
                    }
                    sendEvent(context, UPSCALE_ENABLE_STATUS_CHECKER_FINISHED_EVENT.event(), new StackEvent(stack.getId()));
                } catch (Exception e) {
                    LOGGER.error("Failed to enable the status checker", e);
                    sendEvent(context, UPSCALE_ENABLE_STATUS_CHECKER_FAILED_EVENT.event(),
                            new UpscaleFailureEvent(stack.getId(), "enable status checker", Set.of(), Map.of(), e));
                }
            }
        };
    }

    @Bean(name = "UPSCALE_FINISHED_STATE")
    public Action<?, ?> upscaleFinsihedAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), getUpscaleCompleteStatus(variables), "Upscale complete");
                if (!isRepair(variables)) {
                    SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                    successDetails.getAdditionalDetails().put("Hosts", getUpscaleHosts(variables));
                    operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(successDetails), Collections.emptyList());
                }
                sendEvent(context, UPSCALE_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_FAIL_STATE")
    public Action<?, ?> upscaleFailureAction() {
        return new AbstractUpscaleAction<>(UpscaleFailureEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<UpscaleState, UpscaleFlowEvent> stateContext,
                    UpscaleFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected Object getFailurePayload(UpscaleFailureEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return null;
            }

            @Override
            protected void doExecute(StackContext context, UpscaleFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Upscale failed with payload: " + payload);
                Stack stack = context.getStack();
                String environmentCrn = stack.getEnvironmentCrn();
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                successDetails.getAdditionalDetails()
                        .put(payload.getFailedPhase(), payload.getSuccess() == null ? List.of() : new ArrayList<>(payload.getSuccess()));
                String message = "Upscale failed during " + payload.getFailedPhase();
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                if (payload.getFailureDetails() != null) {
                    failureDetails.getAdditionalDetails().putAll(payload.getFailureDetails());
                }
                String errorReason = payload.getException() == null ? "Unknown error" : payload.getException().getMessage();
                stackUpdater.updateStackStatus(context.getStack().getId(), getFailedStatus(variables), errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                enableStatusChecker(stack, "Failed upscaling FreeIPA");
                sendEvent(context, FAIL_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<UpscaleFailureEvent>> payloadConverters) {
                payloadConverters.add(new UpscaleStackResultToUpscaleFailureEventConverter());
                payloadConverters.add(new CollectMetadataResultToUpscaleFailureEventConverter());
                payloadConverters.add(new BootstrapMachinesFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new HostMetadataSetupFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new InstallFreeIpaServicesFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new PostInstallFreeIpaFailedToUpscaleFailureEventConverter());
            }
        };
    }
}
