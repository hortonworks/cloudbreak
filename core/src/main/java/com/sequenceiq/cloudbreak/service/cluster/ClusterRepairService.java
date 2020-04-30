package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANUALRECOVERY_COULD_NOT_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANUALRECOVERY_NO_NODES_TO_RECOVER;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANUALRECOVERY_REQUESTED;
import static com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName.hostGroupName;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.StateStatus;

@Service
public class ClusterRepairService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepairService.class);

    private static final Long REQUIRED_CM_DATABASE_COUNT = 2L;

    private static final List<String> REATTACH_NOT_SUPPORTED_VOLUME_TYPES = List.of("ephemeral");

    private static final String RECOVERY = "RECOVERY";

    private static final boolean NOT_DELETE_VOLUMES = false;

    private static final boolean NOT_REMOVE_ONLY = false;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private CloudbreakEventService eventService;

    public FlowIdentifier repairAll(Long stackId) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                repair(ManualClusterRepairMode.ALL, stackId, Set.of(), NOT_DELETE_VOLUMES);
        Set<String> repairableHostGroups;
        if (repairStart.isSuccess()) {
            repairableHostGroups = repairStart.getSuccess()
                    .keySet()
                    .stream()
                    .map(HostGroupName::value)
                    .collect(toSet());
        } else {
            repairableHostGroups = Set.of();
        }
        return triggerRepairOrThrowBadRequest(stackId, repairStart, NOT_REMOVE_ONLY, repairableHostGroups);
    }

    public FlowIdentifier repairHostGroups(Long stackId, Set<String> hostGroups, boolean removeOnly) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                repair(ManualClusterRepairMode.HOST_GROUP, stackId, hostGroups, NOT_DELETE_VOLUMES);
        return triggerRepairOrThrowBadRequest(stackId, repairStart, removeOnly, hostGroups);
    }

    public FlowIdentifier repairNodes(Long stackId, Set<String> nodeIds, boolean deleteVolumes, boolean removeOnly) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                repair(ManualClusterRepairMode.NODE_ID, stackId, nodeIds, deleteVolumes);
        return triggerRepairOrThrowBadRequest(stackId, repairStart, removeOnly, nodeIds);
    }

    public Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairWithDryRun(Long stackId) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                repair(ManualClusterRepairMode.DRY_RUN, stackId, Set.of(), NOT_DELETE_VOLUMES);
        boolean repairable = repairStart.isSuccess();
        if (!repairable) {
            LOGGER.info("Stack {} is not repairable. {}", stackId, repairStart.getError().getValidationErrors());
        }
        return repairStart;
    }

    private Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repair(ManualClusterRepairMode repairMode, Long stackId,
            Set<String> selectedParts, boolean deleteVolumes) {
        try {
            return transactionService.required(() -> {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                boolean reattach = !deleteVolumes;
                Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult;
                if (hasPendingFlow(stackId)) {
                    repairStartResult = Result.error(RepairValidation
                            .of("Action cannot be performed because there is an active flow running."));
                } else if (hasStoppedNotSelectedInstance(stack, repairMode, selectedParts)) {
                    repairStartResult = Result.error(RepairValidation
                            .of("Action cannot be performed because there are stopped nodes in the cluster. " +
                                    "Please select them for repair or start the stopped nodes."));
                } else if (reattachNotSupportedOnProvider(stack, reattach)) {
                    repairStartResult = Result.error(RepairValidation
                            .of(String.format("Volume reattach currently not supported on %s platform!", stack.getPlatformVariant())));
                } else {
                    Map<HostGroupName, Set<InstanceMetaData>> repairableNodes = selectRepairableNodes(getInstanceSelectors(repairMode, selectedParts), stack);
                    RepairValidation validationBySelectedNodes = validateSelectedNodes(stack, repairableNodes, reattach);
                    if (validationBySelectedNodes.getValidationErrors().isEmpty()) {
                        setStackStatusAndMarkDeletableVolumes(repairMode, selectedParts, deleteVolumes, stack, repairableNodes);
                        repairStartResult = Result.success(repairableNodes);
                    } else {
                        repairStartResult = Result.error(validationBySelectedNodes);
                    }
                }
                return repairStartResult;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private boolean hasStoppedNotSelectedInstance(Stack stack, ManualClusterRepairMode repairMode, Set<String> selectedParts) {
        if (ManualClusterRepairMode.HOST_GROUP.equals(repairMode)) {
            return stack.getInstanceMetaDataAsList()
                    .stream()
                    .filter(im -> !selectedParts.contains(im.getInstanceGroup().getGroupName()))
                    .anyMatch(im -> InstanceStatus.STOPPED.equals(im.getInstanceStatus()));
        } else if (ManualClusterRepairMode.NODE_ID.equals(repairMode)) {
            return stack.getInstanceMetaDataAsList()
                    .stream()
                    .filter(im -> !selectedParts.contains(im.getInstanceId()))
                    .anyMatch(im -> InstanceStatus.STOPPED.equals(im.getInstanceStatus()));
        }
        return false;
    }

    private boolean hasPendingFlow(Long stackId) {
        return flowLogService.findAllByResourceIdOrderByCreatedDesc(stackId)
                .stream()
                .anyMatch(fl -> StateStatus.PENDING.equals(fl.getStateStatus()));
    }

    private boolean reattachNotSupportedOnProvider(Stack stack, boolean repairWithReattach) {
        return repairWithReattach && !StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant());

    }

    private Pair<Predicate<HostGroup>, Predicate<InstanceMetaData>> getInstanceSelectors(ManualClusterRepairMode repairMode, Set<String> selectedParts) {
        switch (repairMode) {
            case DRY_RUN:
            case ALL:
                return Pair.of(allHostGroup(), allInstances());
            case HOST_GROUP:
                return Pair.of(selectHostGroups(selectedParts), this::selectUnhealthyInstances);
            case NODE_ID:
                return Pair.of(allHostGroup(), selectInstances(selectedParts));
            default:
                throw new IllegalArgumentException("Unknown maunal repair mode " + repairMode);
        }
    }

    private Predicate<HostGroup> allHostGroup() {
        return hostGroup -> true;
    }

    private Predicate<InstanceMetaData> allInstances() {
        return instanceMetaData -> true;
    }

    private Predicate<HostGroup> selectHostGroups(Set<String> hostGroups) {
        return hostGroup -> hostGroups.contains(hostGroup.getName());
    }

    private Predicate<InstanceMetaData> selectInstances(Set<String> nodeIds) {
        return instanceMetaData -> nodeIds.contains(instanceMetaData.getInstanceId());
    }

    private boolean selectUnhealthyInstances(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getInstanceStatus() == InstanceStatus.SERVICES_UNHEALTHY ||
                instanceMetaData.getInstanceStatus() == InstanceStatus.DELETED_ON_PROVIDER_SIDE ||
                instanceMetaData.getInstanceStatus() == InstanceStatus.DELETED_BY_PROVIDER ||
                instanceMetaData.getInstanceStatus() == InstanceStatus.STOPPED;
    }

    private Map<HostGroupName, Set<InstanceMetaData>> selectRepairableNodes(
            Pair<Predicate<HostGroup>, Predicate<InstanceMetaData>> instanceSelectors,
            Stack stack) {
        return hostGroupService.getByCluster(stack.getCluster().getId())
                .stream()
                .filter(hostGroup -> RecoveryMode.MANUAL.equals(hostGroup.getRecoveryMode()))
                .filter(instanceSelectors.getLeft())
                .map(hostGroup -> Map.entry(hostGroupName(hostGroup.getName()), hostGroup
                        .getInstanceGroup()
                        .getNotTerminatedInstanceMetaDataSet()
                        .stream()
                        .filter(instanceSelectors.getRight())
                        .collect(toSet())))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private RepairValidation validateSelectedNodes(Stack stack, Map<HostGroupName, Set<InstanceMetaData>> nodesToRepair, boolean reattach) {
        return new RepairValidation(nodesToRepair
                .entrySet()
                .stream()
                .map(entry -> validateRepairableNodes(stack, entry.getKey(), entry.getValue(), reattach))
                .flatMap(Collection::stream)
                .collect(toList())
        );
    }

    private List<String> validateRepairableNodes(Stack stack, HostGroupName hostGroupName, Set<InstanceMetaData> instances, boolean reattach) {
        List<String> validationResult = new ArrayList<>();
        if (reattach) {
            for (InstanceMetaData instanceMetaData : instances) {
                validationResult.addAll(validateOnGateway(stack, instanceMetaData));
                if (hasReattachSupportedVolumes(stack, hostGroupName.value())) {
                    validationResult.add("Reattach not supported for this disk type.");
                }
            }
        }
        return validationResult;
    }

    private List<String> validateOnGateway(Stack stack, InstanceMetaData instanceMetaData) {
        List<String> validationResult = new ArrayList<>();
        if (isGateway(instanceMetaData)) {
            if (createdFromBaseImage(stack)) {
                validationResult.add("Action is only supported if the image already contains Cloudera Manager and Cloudera Data Platform artifacts.");
            }
            if (!gatewayDatabaseAvailable(stack.getCluster())) {
                validationResult.add("Action is only supported if Cloudera Manager state is stored in external Database.");
            }
        }
        return validationResult;
    }

    private boolean isGateway(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY;
    }

    private boolean createdFromBaseImage(Stack stack) {
        try {
            Image image = componentConfigProviderService.getImage(stack.getId());
            return !imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()).getImage().isPrewarmed();
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private boolean gatewayDatabaseAvailable(Cluster cluster) {
        long cmRdsCount = cluster.getRdsConfigs().stream()
                .filter(rds -> rds.getStatus() == ResourceStatus.USER_MANAGED)
                .map(RDSConfig::getType)
                .filter(type -> DatabaseType.CLOUDERA_MANAGER.name().equals(type)
                        || DatabaseType.CLOUDERA_MANAGER_MANAGEMENT_SERVICE_REPORTS_MANAGER.name().equals(type))
                .distinct()
                .count();
        return cmRdsCount == REQUIRED_CM_DATABASE_COUNT || cluster.getDatabaseServerCrn() != null;
    }

    private boolean hasReattachSupportedVolumes(Stack stack, String hostGroupName) {
        return stack.getInstanceGroupsAsList().stream()
                .filter(instanceGroup -> hostGroupName.equalsIgnoreCase(instanceGroup.getGroupName()))
                .map(InstanceGroup::getTemplate)
                .map(Template::getVolumeTemplates)
                .anyMatch(volumes -> volumes.stream()
                        .map(VolumeTemplate::getVolumeType).anyMatch(REATTACH_NOT_SUPPORTED_VOLUME_TYPES::contains));
    }

    private void updateVolumesDeleteFlag(Stack stack, Predicate<Resource> resourceFilter, boolean deleteVolumes) {
        List<Resource> volumes = stack.getDiskResources().stream()
                .filter(resourceFilter)
                .map(volumeSet -> updateDeleteVolumesFlag(deleteVolumes, volumeSet))
                .collect(toList());
        List<String> volumeNames = volumes.stream().map(Resource::getResourceName).collect(toList());
        LOGGER.info("Update delete volume flag on {} to {}", volumeNames, deleteVolumes);
        resourceService.saveAll(volumes);
    }

    private Predicate<Resource> inInstances(Set<String> instanceIds) {
        return resource -> instanceIds.contains(resource.getInstanceId());
    }

    private Resource updateDeleteVolumesFlag(boolean deleteVolumes, Resource volumeSet) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
        attributes.ifPresent(volumeSetAttributes -> {
            volumeSetAttributes.setDeleteOnTermination(deleteVolumes);
            resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
        });
        return volumeSet;
    }

    private void setStackStatusAndMarkDeletableVolumes(ManualClusterRepairMode repairMode, Set<String> selectedParts, boolean deleteVolumes, Stack stack,
            Map<HostGroupName, Set<InstanceMetaData>> nodesToRepair) {
        if (!ManualClusterRepairMode.DRY_RUN.equals(repairMode)) {
            LOGGER.info("Repair mode is not a dry run, {}", repairMode);
            Predicate<Resource> updateVolumesPredicate = inInstances(nodesToRepair.values()
                    .stream()
                    .flatMap(Collection::stream)
                    .map(InstanceMetaData::getInstanceId)
                    .collect(Collectors.toUnmodifiableSet()));
            updateVolumesDeleteFlag(stack, updateVolumesPredicate, deleteVolumes);
            LOGGER.info("Update stack status to REPAIR_IN_PROGRESS");
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REPAIR_IN_PROGRESS);
        }
    }

    private FlowIdentifier triggerRepairOrThrowBadRequest(Long stackId, Result<Map<HostGroupName, Set<InstanceMetaData>>,
            RepairValidation> repairValidationResult, boolean removeOnly, Set<String> recoveryMessageArgument) {
        if (repairValidationResult.isError()) {
            eventService.fireCloudbreakEvent(stackId, RECOVERY, CLUSTER_MANUALRECOVERY_COULD_NOT_START,
                    repairValidationResult.getError().getValidationErrors());
            throw new BadRequestException(String.join(" ", repairValidationResult.getError().getValidationErrors()));
        } else {
            if (!repairValidationResult.getSuccess().isEmpty()) {
                FlowIdentifier flowIdentifier = flowManager.triggerClusterRepairFlow(stackId, toStringMap(repairValidationResult.getSuccess()),
                        removeOnly);
                eventService.fireCloudbreakEvent(stackId, RECOVERY, CLUSTER_MANUALRECOVERY_REQUESTED, recoveryMessageArgument);
                return flowIdentifier;
            } else {
                eventService.fireCloudbreakEvent(stackId, RECOVERY, CLUSTER_MANUALRECOVERY_NO_NODES_TO_RECOVER, recoveryMessageArgument);
                throw new BadRequestException(String.format("Could not trigger cluster repair  for stack %s, because node list is incorrect", stackId));
            }
        }
    }

    private Map<String, List<String>> toStringMap(Map<HostGroupName, Set<InstanceMetaData>> repairableNodes) {
        return repairableNodes
                .entrySet()
                .stream()
                .collect(toMap(entry -> entry.getKey().value(),
                        entry -> entry.getValue()
                                .stream()
                                .map(InstanceMetaData::getDiscoveryFQDN)
                                .collect(toList())));
    }
}
