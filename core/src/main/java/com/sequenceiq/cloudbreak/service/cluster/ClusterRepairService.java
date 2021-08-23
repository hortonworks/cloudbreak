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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class ClusterRepairService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepairService.class);

    private static final List<String> REATTACH_NOT_SUPPORTED_VOLUME_TYPES = List.of(AwsDiskType.Ephemeral.value());

    private static final String RECOVERY = "RECOVERY";

    private static final String RECOVERY_FAILED = "RECOVERY_FAILED";

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
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private ClusterDBValidationService clusterDBValidationService;

    @Inject
    private RedbeamsClientService redbeamsClientService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private FreeipaService freeipaService;

    @Inject
    private EntitlementService entitlementService;

    public FlowIdentifier repairAll(Long stackId) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.ALL, stackId, Set.of(), false, false);
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
        return triggerRepairOrThrowBadRequest(stackId, repairStart, false, false, repairableHostGroups);
    }

    public FlowIdentifier repairHostGroups(Long stackId, Set<String> hostGroups, boolean removeOnly, boolean restartServices) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.HOST_GROUP, stackId, hostGroups, removeOnly, false);
        return triggerRepairOrThrowBadRequest(stackId, repairStart, removeOnly, restartServices, hostGroups);
    }

    public FlowIdentifier repairNodes(Long stackId, Set<String> nodeIds, boolean deleteVolumes, boolean removeOnly, boolean restartServices) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.NODE_ID, stackId, nodeIds, removeOnly, deleteVolumes);
        return triggerRepairOrThrowBadRequest(stackId, repairStart, removeOnly, restartServices, nodeIds);
    }

    public Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairWithDryRun(Long stackId) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.DRY_RUN, stackId, Set.of(), false, false);
        if (!repairStart.isSuccess()) {
            LOGGER.info("Stack {} is not repairable. {}", stackId, repairStart.getError().getValidationErrors());
        }
        return repairStart;
    }

    public Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validateRepair(ManualClusterRepairMode repairMode, Long stackId,
            Set<String> selectedParts, boolean removeOnly, boolean deleteVolumes) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        boolean reattach = !deleteVolumes;
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult;
        List<String> stoppedInstanceIds = getStoppedNotSelectedInstanceIds(stack, repairMode, selectedParts);
        if (!freeipaService.freeipaStatusInDesiredState(stack, Set.of(Status.AVAILABLE))) {
            repairStartResult = Result.error(RepairValidation
                    .of("Action cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state."));
        } else if (!environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))) {
            repairStartResult = Result.error(RepairValidation
                    .of("Action cannot be performed because the Environment isn't available. Please check the Environment state."));
        } else if (!stoppedInstanceIds.isEmpty()) {
            repairStartResult = Result.error(RepairValidation
                    .of("Action cannot be performed because there are stopped nodes in the cluster. " +
                            "Stopped nodes: [" + String.join(", ", stoppedInstanceIds) + "]. " +
                            "Please select them for repair or start the stopped nodes."));
        } else if (!isReattachSupportedOnProvider(stack, reattach)) {
            repairStartResult = Result.error(RepairValidation
                    .of(String.format("Volume reattach currently not supported on %s platform!", stack.getPlatformVariant())));
        } else if (hasNotAvailableDatabase(stack)) {
            repairStartResult = Result.error(RepairValidation
                    .of(String.format("Database %s is not in AVAILABLE status, could not start repair.", stack.getCluster().getDatabaseServerCrn())));
        } else if (isHAClusterAndRepairNotAllowed(removeOnly, stack)) {
            repairStartResult = Result.error(RepairValidation
                    .of("Repair is not supported when the cluster uses cluster proxy and has multiple gateway nodes. This will be fixed in future releases."));
        } else if (isAnyGWUnhealthyAndItIsNotSelected(repairMode, selectedParts, stack)) {
            repairStartResult = Result.error(RepairValidation.of("Gateway node is unhealthy, it must be repaired first."));
        } else {
            Map<HostGroupName, Set<InstanceMetaData>> repairableNodes = selectRepairableNodes(getInstanceSelectors(repairMode, selectedParts), stack);
            if (repairableNodes.isEmpty()) {
                repairStartResult = Result.error(RepairValidation.of("Repairable node list is empty. Please check node statuses and try again."));
            } else {
                RepairValidation validationBySelectedNodes = validateSelectedNodes(stack, repairableNodes, reattach);
                if (!validationBySelectedNodes.getValidationErrors().isEmpty()) {
                    repairStartResult = Result.error(validationBySelectedNodes);
                } else {
                    setStackStatusAndMarkDeletableVolumes(repairMode, deleteVolumes, stack, repairableNodes);
                    repairStartResult = Result.success(repairableNodes);
                }
            }
        }
        return repairStartResult;
    }

    private boolean isAnyGWUnhealthyAndItIsNotSelected(ManualClusterRepairMode repairMode, Set<String> selectedParts, Stack stack) {
        List<InstanceMetaData> gatewayInstances = stack.getNotTerminatedGatewayInstanceMetadata();
        if (gatewayInstances.size() < 1) {
            LOGGER.info("Stack has no GW");
            return false;
        }
        List<InstanceMetaData> unhealthyGWs = gatewayInstances.stream().filter(gatewayInstance -> !gatewayInstance.isHealthy()).collect(toList());
        if (ManualClusterRepairMode.HOST_GROUP.equals(repairMode)) {
            LOGGER.info("Host group based repair mode, so GW hostgroup should be selected if any GW is not healthy. Unhealthy GWs: {}. Selected instances: {}",
                    unhealthyGWs, selectedParts);
            return unhealthyGWs.stream().anyMatch(unhealthyGW -> !selectedParts.contains(unhealthyGW.getInstanceGroupName()));
        } else if (ManualClusterRepairMode.NODE_ID.equals(repairMode)) {
            LOGGER.info("Node id based repair mode, so GW instance should be selected if it is not healthy. Unhealthy GWs: {}. Selected hostgroups: {}",
                    unhealthyGWs, selectedParts);
            return unhealthyGWs.stream().anyMatch(unhealthyGW -> !selectedParts.contains(unhealthyGW.getInstanceId()));
        } else {
            LOGGER.info("Repair mode is not host group or node id based: {}", repairMode);
            return false;
        }
    }

    private boolean isHAClusterAndRepairNotAllowed(boolean removeOnly, Stack stack) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return !entitlementService.haRepairEnabled(accountId)
                && !entitlementService.haUpgradeEnabled(accountId)
                && !removeOnly
                && stack.getTunnel().useClusterProxy()
                && hasMultipleGatewayInstances(stack);
    }

    private boolean hasMultipleGatewayInstances(Stack stack) {
        int gatewayInstanceCount = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (InstanceGroupType.isGateway(instanceGroup.getInstanceGroupType())) {
                gatewayInstanceCount += instanceGroup.getNodeCount();
            }
        }
        return gatewayInstanceCount > 1;
    }

    private boolean hasNotAvailableDatabase(Stack stack) {
        String databaseServerCrn = stack.getCluster().getDatabaseServerCrn();
        if (StringUtils.isNotBlank(databaseServerCrn)) {
            DatabaseServerV4Response databaseServerResponse = redbeamsClientService.getByCrn(databaseServerCrn);
            if (!databaseServerResponse.getStatus().isAvailable()) {
                return true;
            }
        }
        return false;
    }

    private List<String> getStoppedNotSelectedInstanceIds(Stack stack, ManualClusterRepairMode repairMode, Set<String> selectedParts) {
        if (ManualClusterRepairMode.HOST_GROUP.equals(repairMode)) {
            return stack.getInstanceMetaDataAsList()
                    .stream()
                    .filter(im -> !selectedParts.contains(im.getInstanceGroup().getGroupName()) &&
                            InstanceStatus.STOPPED.equals(im.getInstanceStatus()))
                    .map(InstanceMetaData::getInstanceId)
                    .collect(Collectors.toList());
        } else if (ManualClusterRepairMode.NODE_ID.equals(repairMode)) {
            return stack.getInstanceMetaDataAsList()
                    .stream()
                    .filter(im -> !selectedParts.contains(im.getInstanceId()) &&
                            InstanceStatus.STOPPED.equals(im.getInstanceStatus()))
                    .map(InstanceMetaData::getInstanceId)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean isReattachSupportedOnProvider(Stack stack, boolean repairWithReattach) {
        return !repairWithReattach || StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant());
    }

    private Pair<Predicate<HostGroup>, Predicate<InstanceMetaData>> getInstanceSelectors(ManualClusterRepairMode repairMode, Set<String> selectedParts) {
        switch (repairMode) {
            case DRY_RUN:
            case ALL:
                return Pair.of(allHostGroup(), allInstances());
            case HOST_GROUP:
                return Pair.of(selectHostGroups(selectedParts), this::isUnhealthyInstance);
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

    private boolean isUnhealthyInstance(InstanceMetaData instanceMetaData) {
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
                if (ephemeralStorageOnly(stack, hostGroupName.value())) {
                    validationResult.add("Reattach not supported for this disk type.");
                }
            }
        }
        return validationResult;
    }

    private List<String> validateOnGateway(Stack stack, InstanceMetaData instanceMetaData) {
        List<String> validationResult = new ArrayList<>();
        if (instanceMetaData.isGateway()) {
            if (isCreatedFromBaseImage(stack)) {
                validationResult.add("Action is only supported if the image already contains Cloudera Manager and Cloudera Data Platform artifacts.");
            }
            if (!clusterDBValidationService.isGatewayRepairEnabled(stack.getCluster())) {
                validationResult.add(
                        "Action is only supported if Cloudera Manager state is stored in external Database or the cluster was launched after Mar/16/21.");
            }
        }
        return validationResult;
    }

    private boolean isCreatedFromBaseImage(Stack stack) {
        try {
            Image image = componentConfigProviderService.getImage(stack.getId());
            return !imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()).getImage().isPrewarmed();
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private boolean ephemeralStorageOnly(Stack stack, String hostGroupName) {
        return stack.getInstanceGroupsAsList().stream()
                .filter(instanceGroup -> hostGroupName.equalsIgnoreCase(instanceGroup.getGroupName()))
                .map(InstanceGroup::getTemplate)
                .map(Template::getVolumeTemplates)
                .anyMatch(volumes -> volumes.stream()
                        .map(VolumeTemplate::getVolumeType).allMatch(REATTACH_NOT_SUPPORTED_VOLUME_TYPES::contains));
    }

    private void updateVolumesDeleteFlag(Stack stack, Predicate<Resource> resourceFilter, boolean deleteVolumes) {
        List<Resource> volumes = resourceService.findByStackIdAndType(stack.getId(), stack.getDiskResourceType());
        volumes = volumes.stream()
                .filter(resourceFilter)
                .map(volumeSet -> updateDeleteVolumesFlag(deleteVolumes, volumeSet))
                .collect(toList());
        List<String> volumeNames = volumes.stream().map(Resource::getResourceName).collect(toList());
        LOGGER.info("Update delete volume flag on {} to {}", volumeNames, deleteVolumes);
        resourceService.saveAll(volumes);
    }

    private Predicate<Resource> inInstances(Set<String> instanceIds) {
        return resource -> {
            if (resource.getInstanceId() != null) {
                return instanceIds.contains(resource.getInstanceId());
            } else {
                return false;
            }
        };
    }

    private Resource updateDeleteVolumesFlag(boolean deleteVolumes, Resource volumeSet) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
        attributes.ifPresent(volumeSetAttributes -> {
            volumeSetAttributes.setDeleteOnTermination(deleteVolumes);
            resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
        });
        return volumeSet;
    }

    private void setStackStatusAndMarkDeletableVolumes(ManualClusterRepairMode repairMode, boolean deleteVolumes, Stack stack,
            Map<HostGroupName, Set<InstanceMetaData>> nodesToRepair) {
        if (!ManualClusterRepairMode.DRY_RUN.equals(repairMode) && !nodesToRepair.isEmpty()) {
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
            RepairValidation> repairValidationResult, boolean removeOnly, boolean restartServices, Set<String> recoveryMessageArgument) {
        if (repairValidationResult.isError()) {
            eventService.fireCloudbreakEvent(stackId, RECOVERY_FAILED, CLUSTER_MANUALRECOVERY_COULD_NOT_START,
                    repairValidationResult.getError().getValidationErrors());
            throw new BadRequestException(String.join(" ", repairValidationResult.getError().getValidationErrors()));
        } else {
            if (!repairValidationResult.getSuccess().isEmpty()) {
                FlowIdentifier flowIdentifier = flowManager.triggerClusterRepairFlow(stackId, toStringMap(repairValidationResult.getSuccess()),
                        removeOnly, restartServices);
                eventService.fireCloudbreakEvent(stackId, RECOVERY, CLUSTER_MANUALRECOVERY_REQUESTED,
                        List.of(String.join(",", recoveryMessageArgument)));
                return flowIdentifier;
            } else {
                eventService.fireCloudbreakEvent(stackId, RECOVERY_FAILED, CLUSTER_MANUALRECOVERY_NO_NODES_TO_RECOVER, recoveryMessageArgument);
                throw new BadRequestException(String.format("Could not trigger cluster repair for stack %s because node list is incorrect", stackId));
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
