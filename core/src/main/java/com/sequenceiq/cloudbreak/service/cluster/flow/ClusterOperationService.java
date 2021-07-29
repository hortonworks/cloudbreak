package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AUTORECOVERY_REQUESTED_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AUTORECOVERY_REQUESTED_HOST_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_REPORTED_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_REPORTED_HOST_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RECOVERED_NODES_REPORTED_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RECOVERED_NODES_REPORTED_HOST_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_IGNORED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_IGNORED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
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
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.util.NotAllowedStatusUpdate;
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

    @Inject
    private StackStopRestrictionService stackStopRestrictionService;

    @Measure(ClusterOperationService.class)
    public Cluster create(Stack stack, Cluster cluster, List<ClusterComponent> components, User user) throws TransactionService.TransactionExecutionException {
        LOGGER.debug("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        String stackName = stack.getName();
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.getCluster().getName()));
        }
        long start = System.currentTimeMillis();
        return transactionService.required(() -> {
            setWorkspace(cluster, stack.getWorkspace());
            cluster.setEnvironmentCrn(stack.getEnvironmentCrn());

            if (Status.CREATE_FAILED.equals(stack.getStatus())) {
                throw new BadRequestException("Stack creation failed, cannot create cluster.");
            }
            if (cluster.getFileSystem() != null) {
                cluster.setFileSystem(fileSystemConfigService.createWithMdcContextRestore(cluster.getFileSystem(), cluster.getWorkspace(), user));
            }

            removeGatewayIfNotSupported(cluster, components);

            cluster.setStack(stack);
            stack.setCluster(cluster);

            Cluster savedCluster = measure(() -> clusterService.saveClusterAndComponent(cluster, components, stackName),
                    LOGGER,
                    "saveClusterAndComponent {} ms");
            measure(() -> usageLoggingUtil.logClusterRequestedUsageEvent(cluster),
                    LOGGER,
                    "logClusterRequestedUsageEvent {} ms");
            LOGGER.info("cluster saved {} ms", System.currentTimeMillis() - start);
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

    public void reportHealthChange(String crn, Map<String, Optional<String>> failedNodes, Set<String> newHealthyNodes) {
        if (!Sets.intersection(failedNodes.keySet(), newHealthyNodes).isEmpty()) {
            throw new BadRequestException("Failed nodes " + failedNodes.keySet() + " and healthy nodes " + newHealthyNodes + " should not have common items.");
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

    private void handleHealthChange(Map<String, Optional<String>> failedNodesWithReason, Set<String> newHealthyNodes, Stack stack) {
        Cluster cluster = stack.getCluster();
        Set<InstanceMetaData> notTerminatedInstanceMetadataSet = instanceMetaDataService.getAllInstanceMetadataByStackId(stack.getId()).stream()
                .filter(i -> !i.isTerminated())
                .collect(Collectors.toSet());
        Map<InstanceMetaData, Optional<String>> failedInstanceMetadataMap = notTerminatedInstanceMetadataSet.stream()
                .filter(i -> failedNodesWithReason.containsKey(i.getDiscoveryFQDN()))
                .collect(Collectors.toMap(imd -> imd, imd -> failedNodesWithReason.get(imd.getDiscoveryFQDN())));
        Map<String, HostGroup> hostGroupsInCluster = hostGroupService.findHostGroupsInCluster(cluster.getId()).stream()
                .collect(Collectors.toMap(HostGroup::getName, h -> h));
        Map<String, List<String>> autoRecoveryNodesMap = new HashMap<>();
        Map<String, InstanceMetaData> autoRecoveryMetadata = new HashMap<>();
        failedInstanceMetadataMap.entrySet().stream()
                .filter(entry -> recoveryModeMatches(hostGroupsInCluster, entry, RecoveryMode.AUTO))
                .forEach(entry -> {
                    validateRepair(stack, entry.getKey());
                    prepareForAutoRecovery(stack, autoRecoveryNodesMap, autoRecoveryMetadata, entry.getKey().getDiscoveryFQDN(),
                            entry.getKey(), hostGroupsInCluster.get(entry.getKey().getInstanceGroupName()).getName());
                });
        Map<InstanceMetaData, Optional<String>> failedMetaData = failedInstanceMetadataMap.entrySet().stream()
                .filter(entry -> recoveryModeMatches(hostGroupsInCluster, entry, RecoveryMode.MANUAL))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        logUnknownNodes(failedNodesWithReason, notTerminatedInstanceMetadataSet);
        handleChangedHosts(cluster, newHealthyNodes, autoRecoveryNodesMap, autoRecoveryMetadata, failedMetaData);
    }

    private boolean recoveryModeMatches(Map<String, HostGroup> hostGroupsInCluster, Map.Entry<InstanceMetaData, Optional<String>> entry,
            RecoveryMode recoveryMode) {
        String instanceGroupName = entry.getKey().getInstanceGroupName();
        return hostGroupsInCluster.containsKey(instanceGroupName) && recoveryMode.equals(hostGroupsInCluster.get(instanceGroupName).getRecoveryMode());
    }

    private void logUnknownNodes(Map<String, Optional<String>> failedNodes, Set<InstanceMetaData> notTerminatedInstanceMetadataSet) {
        Set<String> unknownNodes = notTerminatedInstanceMetadataSet.stream()
                .filter(i -> !failedNodes.containsKey(i.getDiscoveryFQDN()))
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toSet());
        LOGGER.error("No metadata information for the nodes: " + unknownNodes);
    }

    private void handleChangedHosts(Cluster cluster, Set<String> newHealthyNodes,
            Map<String, List<String>> autoRecoveryNodesMap, Map<String, InstanceMetaData> autoRecoveryHostMetadata,
            Map<InstanceMetaData, Optional<String>> failedHostMetadata) {
        try {
            updateAutoRecoverableNodes(cluster, autoRecoveryNodesMap, autoRecoveryHostMetadata);
            updateFailedNodes(cluster, failedHostMetadata);
            updateNewHealthyNodes(cluster, newHealthyNodes);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void updateAutoRecoverableNodes(Cluster cluster, Map<String, List<String>> autoRecoveryNodesMap,
            Map<String, InstanceMetaData> autoRecoveryHostMetadata) throws TransactionExecutionException {
        if (!autoRecoveryNodesMap.isEmpty()) {
            flowManager.triggerClusterRepairFlow(cluster.getStack().getId(), autoRecoveryNodesMap, false, false);
            Map<String, Optional<String>> hostNamesWithReason = autoRecoveryHostMetadata.keySet().stream()
                    .collect(Collectors.toMap(host -> host, host -> Optional.empty()));
            Set<InstanceStatus> expectedStates = Set.of(SERVICES_HEALTHY);
            InstanceStatus newState = InstanceStatus.WAITING_FOR_REPAIR;
            ResourceEvent clusterEvent = CLUSTER_AUTORECOVERY_REQUESTED_CLUSTER_EVENT;
            ResourceEvent hostEvent = CLUSTER_AUTORECOVERY_REQUESTED_HOST_EVENT;
            updateChangedHosts(cluster, hostNamesWithReason, expectedStates, newState, clusterEvent, hostEvent);
        }
    }

    private void updateFailedNodes(Cluster cluster, Map<InstanceMetaData, Optional<String>> failedHostMetadata) throws TransactionExecutionException {
        if (!failedHostMetadata.isEmpty()) {
            Map<String, Optional<String>> hostNamesWithReason = failedHostMetadata.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().getDiscoveryFQDN(), e -> e.getValue()));
            Set<InstanceStatus> expectedStates = Set.of(SERVICES_HEALTHY, SERVICES_RUNNING);
            InstanceStatus newState = SERVICES_UNHEALTHY;
            ResourceEvent clusterEvent = CLUSTER_FAILED_NODES_REPORTED_CLUSTER_EVENT;
            ResourceEvent hostEvent = CLUSTER_FAILED_NODES_REPORTED_HOST_EVENT;
            updateChangedHosts(cluster, hostNamesWithReason, expectedStates, newState, clusterEvent, hostEvent);
        }
    }

    private void updateNewHealthyNodes(Cluster cluster, Set<String> newHealthyNodes) throws TransactionExecutionException {
        if (!newHealthyNodes.isEmpty()) {
            Map<String, Optional<String>> hostNamesWithReason = newHealthyNodes.stream().collect(Collectors.toMap(host -> host, host -> Optional.empty()));
            Set<InstanceStatus> expectedStates = Set.of(SERVICES_UNHEALTHY, SERVICES_RUNNING);
            InstanceStatus newState = SERVICES_HEALTHY;
            ResourceEvent clusterEvent = CLUSTER_RECOVERED_NODES_REPORTED_CLUSTER_EVENT;
            ResourceEvent hostEvent = CLUSTER_RECOVERED_NODES_REPORTED_HOST_EVENT;
            updateChangedHosts(cluster, hostNamesWithReason, expectedStates, newState, clusterEvent, hostEvent);
        }
    }

    private void updateChangedHosts(Cluster cluster, Map<String, Optional<String>> hostNamesWithReason, Set<InstanceStatus> expectedState,
            InstanceStatus newState, ResourceEvent clusterEvent, ResourceEvent hostEvent) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            Collection<InstanceMetaData> changedHosts = collectChangedHosts(cluster, hostNamesWithReason, expectedState, newState, hostEvent);
            updateChangedHostsInstanceMetadata(cluster, hostNamesWithReason, newState, clusterEvent, changedHosts);
            return null;
        });
    }

    private Collection<InstanceMetaData> collectChangedHosts(Cluster cluster, Map<String, Optional<String>> hostNamesWithReason,
            Set<InstanceStatus> expectedState, InstanceStatus newState, ResourceEvent hostEvent) {
        return instanceMetaDataService.findNotTerminatedForStack(cluster.getStack().getId()).stream()
                .filter(host -> expectedState.contains(host.getInstanceStatus()) && hostNamesWithReason.containsKey(host.getDiscoveryFQDN()))
                .map(host -> updateHostStatus(hostNamesWithReason, newState, hostEvent, host))
                .collect(Collectors.toSet());
    }

    private InstanceMetaData updateHostStatus(Map<String, Optional<String>> hostNamesWithReason, InstanceStatus newState,
            ResourceEvent hostEvent, InstanceMetaData host) {
        host.setInstanceStatus(newState);
        if (hostNamesWithReason.get(host.getDiscoveryFQDN()).isPresent()) {
            host.setStatusReason(hostNamesWithReason.get(host.getDiscoveryFQDN()).get());
        } else {
            String hostMessage = cloudbreakMessagesService.getMessage(hostEvent.getMessage(), List.of(host.getDiscoveryFQDN()));
            host.setStatusReason(hostMessage);
        }
        return host;
    }

    private void updateChangedHostsInstanceMetadata(Cluster cluster, Map<String, Optional<String>> hostNamesWithReason,
            InstanceStatus newState, ResourceEvent clusterEvent, Collection<InstanceMetaData> changedHosts) {
        if (!changedHosts.isEmpty()) {
            String messageArgument = getMessageArgument(hostNamesWithReason);
            LOGGER.info(cloudbreakMessagesService.getMessage(clusterEvent.getMessage(), List.of(messageArgument)));
            String eventType = SERVICES_HEALTHY.equals(newState) ? AVAILABLE.name() : RECOVERY;
            eventService.fireCloudbreakEvent(cluster.getStack().getId(), eventType, clusterEvent, List.of(messageArgument));
            instanceMetaDataService.saveAll(changedHosts);
        }
    }

    private String getMessageArgument(Map<String, Optional<String>> hostNamesWithReason) {
        return hostNamesWithReason.entrySet().stream()
                .map(this::mapHostNameWithReasonToMessage)
                .collect(Collectors.joining(", "));
    }

    private String mapHostNameWithReasonToMessage(Map.Entry<String, Optional<String>> entry) {
        if (entry.getValue().isPresent()) {
            return String.format("%s: [%s]", entry.getKey(), entry.getValue().get());
        } else {
            return entry.getKey();
        }
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
        StopRestrictionReason reason = stackStopRestrictionService.isInfrastructureStoppable(stack.getCloudPlatform(), stack.getInstanceGroups());
        FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();
        if (cluster.isStopped()) {
            eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), CLUSTER_STOP_IGNORED);
        } else if (reason != StopRestrictionReason.NONE) {
            throw new BadRequestException(
                    String.format("Cannot stop a cluster '%s'. Reason: %s", cluster.getId(), reason.getReason()));
        } else if (!cluster.isClusterReadyForStop() && !cluster.isStopFailed()) {
            throw NotAllowedStatusUpdate
                    .cluster(stack)
                    .to(STOPPED)
                    .expectedIn(AVAILABLE)
                    .badRequest();
        } else if (!stack.isStackReadyForStop() && !stack.isStopFailed()) {
            throw NotAllowedStatusUpdate
                    .cluster(stack)
                    .to(STOPPED)
                    .expectedIn(AVAILABLE)
                    .badRequest();
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
                throw NotAllowedStatusUpdate
                        .cluster(stack)
                        .to(START_REQUESTED)
                        .expectedIn(STOPPED)
                        .badRequest();
            } else if (!stack.isAvailable() && !cluster.isStartFailed()) {
                throw NotAllowedStatusUpdate
                        .cluster(stack)
                        .to(START_REQUESTED)
                        .expectedIn(AVAILABLE)
                        .badRequest();
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

    public FlowIdentifier updateSalt(Stack stack) {
        return flowManager.triggerSaltUpdate(stack.getId());
    }

    public FlowIdentifier updatePillarConfiguration(Stack stack) {
        return flowManager.triggerPillarConfigurationUpdate(stack.getId());
    }

    public FlowIdentifier rotateAutoTlsCertificates(Stack stack, CertificatesRotationV4Request certificatesRotationV4Request) {
        return flowManager.triggerAutoTlsCertificatesRotation(stack.getId(), certificatesRotationV4Request);
    }
}
