package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AUTORECOVERY_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_REPORTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RECOVERED_NODES_REPORTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_IGNORED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_IGNORED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ClusterOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterOperationService.class);

    private static final String RECOVERY = "RECOVERY";

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ClusterService clusterService;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private UsageLoggingUtil usageLoggingUtil;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private UpdateHostsValidator updateHostsValidator;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintValidatorFactory blueprintValidatorFactory;

    @Measure(ClusterOperationService.class)
    public Cluster create(Stack stack, Cluster cluster, List<ClusterComponent> components, User user) throws TransactionService.TransactionExecutionException {
        LOGGER.debug("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        String stackName = stack.getName();
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.getCluster().getName()));
        }
        return transactionService.required(() -> {
            setWorkspace(cluster, stack.getWorkspace());
            cluster.setEnvironmentCrn(stack.getEnvironmentCrn());

            long start = System.currentTimeMillis();
            if (clusterService.findByNameAndWorkspace(cluster.getName(), stack.getWorkspace()).isPresent()) {
                throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName());
            }
            LOGGER.debug("Cluster name collision check took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            if (Status.CREATE_FAILED.equals(stack.getStatus())) {
                throw new BadRequestException("Stack creation failed, cannot create cluster.");
            }

            start = System.currentTimeMillis();
            LOGGER.debug("Host group constrainst saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            if (cluster.getFileSystem() != null) {
                cluster.setFileSystem(fileSystemConfigService.createWithMdcContextRestore(cluster.getFileSystem(), cluster.getWorkspace(), user));
            }
            LOGGER.debug("Filesystem config saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            removeGatewayIfNotSupported(cluster, components);

            cluster.setStack(stack);
            stack.setCluster(cluster);

            Cluster savedCluster = clusterService.saveClusterAndComponent(cluster, components, stackName);
            usageLoggingUtil.logClusterRequestedUsageEvent(cluster);
            if (stack.isAvailable()) {
                flowManager.triggerClusterInstall(stack.getId());
                InMemoryStateStore.putCluster(savedCluster.getId(), statusToPollGroupConverter.convert(savedCluster.getStatus()));
                if (InMemoryStateStore.getStack(stack.getId()) == null) {
                    InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
                }
            }
            return savedCluster;
        });
    }

    private void setWorkspace(Cluster cluster, Workspace workspace) {
        cluster.setWorkspace(workspace);
        if (cluster.getGateway() != null) {
            cluster.getGateway().setWorkspace(workspace);
        }
    }

    private void removeGatewayIfNotSupported(Cluster cluster, List<ClusterComponent> components) {
        Optional<ClusterComponent> cmRepoOpt = components.stream().filter(cmp -> ComponentType.CM_REPO_DETAILS.equals(cmp.getComponentType())).findFirst();
        if (cmRepoOpt.isPresent()) {
            try {
                ClouderaManagerRepo cmRepo = cmRepoOpt.get().getAttributes().get(ClouderaManagerRepo.class);
                if (!CMRepositoryVersionUtil.isKnoxGatewaySupported(cmRepo)) {
                    LOGGER.debug("Knox gateway is not supported by CM version: {}, ignoring it for cluster: {}", cmRepo.getVersion(), cluster.getName());
                    cluster.setGateway(null);
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to read CM repo cluster component", e);
            }
        }
    }

    public void delete(Long stackId, boolean forced) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stackId)));
        LOGGER.debug("Cluster delete requested.");
        markVolumesForDeletion(stack);
        flowManager.triggerClusterTermination(stack, forced, ThreadBasedUserCrnProvider.getUserCrn());
    }

    private void markVolumesForDeletion(Stack stack) {
        if (!StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant())) {
            return;
        }
        LOGGER.debug("Mark volumes for delete on termination in case of active repair flow.");
        try {
            transactionService.required(() -> {
                List<Resource> resources = stack.getResourcesByType(ResourceType.AWS_ENCRYPTED_VOLUME);
                resources.forEach(resource -> updateDeleteVolumesFlag(Boolean.TRUE, resource));
                return resourceService.saveAll(resources);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private Resource updateDeleteVolumesFlag(boolean deleteVolumes, Resource volumeSet) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
        attributes.ifPresent(volumeSetAttributes -> {
            volumeSetAttributes.setDeleteOnTermination(deleteVolumes);
            resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
        });
        return volumeSet;
    }

    public FlowIdentifier updateHosts(Long stackId, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stack.getName()));
        }
        boolean downscaleRequest = updateHostsValidator.validateRequest(stack, hostGroupAdjustment);
        if (downscaleRequest) {
            clusterService.updateClusterStatusByStackId(stackId, UPDATE_REQUESTED);
            return flowManager.triggerClusterDownscale(stackId, hostGroupAdjustment);
        } else {
            return flowManager.triggerClusterUpscale(stackId, hostGroupAdjustment);
        }
    }

    public FlowIdentifier updateUserNamePassword(Long stackId, UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        String oldUserName = cluster.getUserName();
        String oldPassword = cluster.getPassword();
        String newUserName = userNamePasswordJson.getUserName();
        String newPassword = userNamePasswordJson.getPassword();
        if (!newUserName.equals(oldUserName)) {
            return flowManager.triggerClusterCredentialReplace(stack.getId(), userNamePasswordJson.getUserName(), userNamePasswordJson.getPassword());
        } else if (!newPassword.equals(oldPassword)) {
            return flowManager.triggerClusterCredentialUpdate(stack.getId(), userNamePasswordJson.getPassword());
        } else {
            throw new BadRequestException("The request may not change credential");
        }
    }

    public void reportHealthChange(String crn, Set<String> failedNodes, Set<String> newHealthyNodes) {
        if (!Sets.intersection(failedNodes, newHealthyNodes).isEmpty()) {
            throw new BadRequestException("Failed nodes " + failedNodes + " and healthy nodes " + newHealthyNodes + " should not have common items.");
        }
        if (failedNodes.isEmpty() && newHealthyNodes.isEmpty()) {
            return;
        }
        try {
            transactionService.required(() -> {
                Stack stack = stackService.findByCrn(crn);
                if (stack != null && !stack.getStatus().isInProgress()) {
                    handleHealthChange(failedNodes, newHealthyNodes, stack);
                } else {
                    LOGGER.debug("Stack [{}] status is {}, thus we do not handle failure report.", stack.getName(), stack.getStatus());
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void handleHealthChange(Set<String> failedNodes, Set<String> newHealthyNodes, Stack stack) {
        Cluster cluster = stack.getCluster();
        Map<String, List<String>> autoRecoveryNodesMap = new HashMap<>();
        Map<String, InstanceMetaData> autoRecoveryMetadata = new HashMap<>();
        Map<String, InstanceMetaData> failedMetaData = new HashMap<>();
        for (String failedNode : failedNodes) {
            instanceMetaDataService.findHostInStack(stack.getId(), failedNode).ifPresentOrElse(instanceMetaData -> {
                hostGroupService.getRepairViewByClusterIdAndName(cluster.getId(), instanceMetaData.getInstanceGroupName()).ifPresent(hostGroup -> {
                    if (hostGroup.getRecoveryMode() == RecoveryMode.AUTO) {
                        validateRepair(stack, instanceMetaData);
                        prepareForAutoRecovery(stack, autoRecoveryNodesMap, autoRecoveryMetadata, failedNode, instanceMetaData, hostGroup.getName());
                    } else if (hostGroup.getRecoveryMode() == RecoveryMode.MANUAL) {
                        failedMetaData.put(failedNode, instanceMetaData);
                    }
                });
            }, () -> LOGGER.error("No metadata information for the node: " + failedNode));
        }
        handleChangedHosts(cluster, newHealthyNodes, autoRecoveryNodesMap, autoRecoveryMetadata, failedMetaData);
    }

    private void handleChangedHosts(Cluster cluster, Set<String> newHealthyNodes,
                                    Map<String, List<String>> autoRecoveryNodesMap, Map<String, InstanceMetaData> autoRecoveryHostMetadata,
                                    Map<String, InstanceMetaData> failedHostMetadata) {
        try {
            boolean hasAutoRecoverableNodes = !autoRecoveryNodesMap.isEmpty();
            if (hasAutoRecoverableNodes) {
                flowManager.triggerClusterRepairFlow(cluster.getStack().getId(), autoRecoveryNodesMap, false);
                updateChangedHosts(cluster, autoRecoveryHostMetadata.keySet(), Set.of(SERVICES_HEALTHY), InstanceStatus.WAITING_FOR_REPAIR,
                        CLUSTER_AUTORECOVERY_REQUESTED);
            }
            if (!failedHostMetadata.isEmpty()) {
                updateChangedHosts(cluster, failedHostMetadata.keySet(), Set.of(SERVICES_HEALTHY, SERVICES_RUNNING), SERVICES_UNHEALTHY,
                        CLUSTER_FAILED_NODES_REPORTED);
            }
            if (!newHealthyNodes.isEmpty()) {
                updateChangedHosts(cluster, newHealthyNodes, Set.of(SERVICES_UNHEALTHY, SERVICES_RUNNING), SERVICES_HEALTHY,
                        CLUSTER_RECOVERED_NODES_REPORTED);
            }
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void updateChangedHosts(Cluster cluster, Set<String> hostNames, Set<InstanceStatus> expectedState,
                                    InstanceStatus newState, ResourceEvent resourceEvent) throws TransactionService.TransactionExecutionException {
        String recoveryMessage = cloudbreakMessagesService.getMessage(resourceEvent.getMessage(), hostNames);
        Set<InstanceMetaData> notTerminatedInstanceMetaDatasForStack = instanceMetaDataService.findNotTerminatedForStack(cluster.getStack().getId());
        Collection<InstanceMetaData> changedHosts = new HashSet<>();
        transactionService.required(() -> {
            for (InstanceMetaData host : notTerminatedInstanceMetaDatasForStack) {
                if (expectedState.contains(host.getInstanceStatus()) && hostNames.contains(host.getDiscoveryFQDN())) {
                    host.setInstanceStatus(newState);
                    host.setStatusReason(recoveryMessage);
                    changedHosts.add(host);
                }
            }
            if (!changedHosts.isEmpty()) {
                LOGGER.info(recoveryMessage);
                String eventType;
                if (SERVICES_HEALTHY.equals(newState)) {
                    eventType = AVAILABLE.name();
                } else {
                    eventType = RECOVERY;
                }
                eventService.fireCloudbreakEvent(cluster.getStack().getId(), eventType, resourceEvent, List.of(String.join(",", hostNames)));
                instanceMetaDataService.saveAll(changedHosts);
            }
            return null;
        });
    }

    private void validateRepair(Stack stack, InstanceMetaData instanceMetaData) {
        if (isGateway(instanceMetaData) && clusterService.withEmbeddedClusterManagerDB(stack.getCluster())) {
            throw new BadRequestException("Cluster manager server failure with embedded database cannot be repaired!");
        }
    }

    private boolean isGateway(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY;
    }

    private void prepareForAutoRecovery(Stack stack,
                                        Map<String, List<String>> autoRecoveryNodesMap,
                                        Map<String, InstanceMetaData> autoRecoveryHostMetadata,
                                        String failedNode,
                                        InstanceMetaData hostMetadata,
                                        String hostGroupName) {
        List<String> nodeList = autoRecoveryNodesMap.get(hostGroupName);
        if (nodeList == null) {
            updateHostsValidator.validateComponentsCategory(stack, hostGroupName);
            nodeList = new ArrayList<>();
            autoRecoveryNodesMap.put(hostGroupName, nodeList);
        }
        nodeList.add(failedNode);
        autoRecoveryHostMetadata.put(failedNode, hostMetadata);
    }

    private FlowIdentifier sync(Stack stack) {
        return flowManager.triggerClusterSync(stack.getId());
    }

    public FlowIdentifier updateStatus(Long stackId, StatusRequest statusRequest) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        return updateStatus(stack, statusRequest);
    }

    public FlowIdentifier updateStatus(Stack stack, StatusRequest statusRequest) {
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stack.getName()));
        }
        FlowIdentifier flowIdentifier;
        switch (statusRequest) {
            case SYNC:
                flowIdentifier = sync(stack);
                break;
            case STOPPED:
                flowIdentifier = stop(stack, cluster);
                break;
            case STARTED:
                flowIdentifier = start(stack, cluster);
                break;
            default:
                throw new BadRequestException("Cannot update the status of cluster because status request not valid");
        }
        return flowIdentifier;
    }

    private FlowIdentifier stop(Stack stack, Cluster cluster) {
        StopRestrictionReason reason = stack.isInfrastructureStoppable();
        FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();
        if (cluster.isStopped()) {
            eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), CLUSTER_STOP_IGNORED);
        } else if (reason != StopRestrictionReason.NONE) {
            throw new BadRequestException(
                    String.format("Cannot stop a cluster '%s'. Reason: %s", cluster.getId(), reason.getReason()));
        } else if (!cluster.isClusterReadyForStop() && !cluster.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STOPPED, because it isn't in AVAILABLE state.", cluster.getId()));
        } else if (!stack.isStackReadyForStop() && !stack.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", cluster.getId()));
        } else if (cluster.isAvailable() || cluster.isStopFailed()) {
            clusterService.updateClusterStatusByStackId(stack.getId(), STOP_REQUESTED);
            flowIdentifier = flowManager.triggerClusterStop(stack.getId());
        }
        return flowIdentifier;
    }

    private FlowIdentifier start(Stack stack, Cluster cluster) {
        FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();
        if (stack.isStartInProgress()) {
            eventService.fireCloudbreakEvent(stack.getId(), START_REQUESTED.name(), CLUSTER_START_REQUESTED);
            clusterService.updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
        } else {
            if (cluster.isAvailable()) {
                eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), CLUSTER_START_IGNORED);
            } else if (!cluster.isClusterReadyForStart() && !cluster.isStartFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STARTED, because it isn't in STOPPED state.", cluster.getId()));
            } else if (!stack.isAvailable() && !cluster.isStartFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", cluster.getId()));
            } else {
                clusterService.updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
                flowIdentifier = flowManager.triggerClusterStart(stack.getId());
            }
        }
        return flowIdentifier;
    }

    private FlowIdentifier triggerClusterInstall(Stack stack) {
        return flowManager.triggerClusterReInstall(stack.getId());
    }

    public FlowIdentifier recreate(Stack stack, String blueprintName, Set<HostGroup> hostGroups, boolean validateBlueprint)
            throws TransactionExecutionException {
        return transactionService.required(() -> {
            checkBlueprintIdAndHostGroups(blueprintName, hostGroups);
            Stack stackWithLists = stackService.getByIdWithListsInTransaction(stack.getId());
            Cluster cluster = clusterService.getCluster(stackWithLists);
            Blueprint blueprint = blueprintService.getByNameForWorkspace(blueprintName, stack.getWorkspace());
            if (!clusterService.withEmbeddedClusterManagerDB(cluster)) {
                throw new BadRequestException("Cluster Manager doesn't support resetting external DB automatically. To reset Cluster Manager schema you "
                        + "must first drop and then create it using DDL scripts from /var/lib/ambari-server/resources or /opt/cloudera/cm/schema/postgresql/");
            }
            BlueprintValidator blueprintValidator = blueprintValidatorFactory.createBlueprintValidator(blueprint);
            blueprintValidator.validate(blueprint, hostGroups, stackWithLists.getInstanceGroups(), validateBlueprint);

            clusterService.prepareCluster(hostGroups, blueprint, stackWithLists, cluster);
            return triggerClusterInstall(stackWithLists);
        });
    }

    private void checkBlueprintIdAndHostGroups(String blueprint, Set<HostGroup> hostGroups) {
        if (blueprint == null || hostGroups == null) {
            throw new BadRequestException("Cluster definition id and hostGroup assignments can not be null.");
        }
    }

    public FlowIdentifier triggerMaintenanceModeValidation(Stack stack) {
        return flowManager.triggerMaintenanceModeValidationFlow(stack.getId());
    }
}