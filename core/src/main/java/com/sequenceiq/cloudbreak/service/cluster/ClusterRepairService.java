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

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
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
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class ClusterRepairService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepairService.class);

    private static final String RECOVERY = "RECOVERY";

    private static final String RECOVERY_FAILED = "RECOVERY_FAILED";

    @Inject
    private StackDtoService stackDtoService;

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

    @Inject
    private StackStopRestrictionService stackStopRestrictionService;

    @Inject
    private StackUpgradeService stackUpgradeService;

    @Inject
    private SaltVersionUpgradeService saltVersionUpgradeService;

    public FlowIdentifier repairAll(StackView stackView, boolean upgrade, boolean keepVariant) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.ALL, stackView.getId(), Set.of(), false);
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
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String upgradeVariant = stackUpgradeService.calculateUpgradeVariant(stackView, userCrn, keepVariant);
        return triggerRepairOrThrowBadRequest(
                stackView.getId(),
                repairStart,
                RepairType.ALL_AT_ONCE,
                false,
                repairableHostGroups,
                upgradeVariant,
                upgrade);
    }

    public FlowIdentifier repairHostGroups(Long stackId, Set<String> hostGroups, boolean restartServices) {
        StackDto stack = stackDtoService.getById(stackId);
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.HOST_GROUP, stack, hostGroups, false);
        String upgradeVariant = stackUpgradeService.calculateUpgradeVariant(stack, ThreadBasedUserCrnProvider.getUserCrn(), Boolean.FALSE, repairStart);
        return triggerRepairOrThrowBadRequest(stackId, repairStart, RepairType.ALL_AT_ONCE, restartServices, hostGroups, upgradeVariant, false);
    }

    public FlowIdentifier repairNodes(Long stackId, Set<String> nodeIds, boolean deleteVolumes, boolean restartServices) {
        StackDto stack = stackDtoService.getById(stackId);
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.NODE_ID, stack, nodeIds, deleteVolumes);
        String upgradeVariant = stackUpgradeService.calculateUpgradeVariant(stack, ThreadBasedUserCrnProvider.getUserCrn(), Boolean.FALSE, repairStart);
        return triggerRepairOrThrowBadRequest(stackId, repairStart, RepairType.ALL_AT_ONCE, restartServices, nodeIds, upgradeVariant, false);
    }

    public Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairWithDryRun(Long stackId) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStart =
                validateRepair(ManualClusterRepairMode.DRY_RUN, stackId, Set.of(), false);
        if (!repairStart.isSuccess()) {
            LOGGER.info("Stack {} is not repairable. {}", stackId, repairStart.getError().getValidationErrors());
        }
        return repairStart;
    }

    public Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validateRepair(ManualClusterRepairMode repairMode, Long stackId,
            Set<String> selectedParts, boolean deleteVolumes) {
        StackDto stack = stackDtoService.getById(stackId);
        return validateRepair(repairMode, stack, selectedParts, deleteVolumes);
    }

    public Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validateRepair(ManualClusterRepairMode repairMode, StackDto stack,
            Set<String> selectedParts, boolean deleteVolumes) {
        boolean reattach = !deleteVolumes;
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult;
        Optional<RepairValidation> repairValidationError = validateRepairConditions(repairMode, stack, selectedParts);
        if (repairValidationError.isPresent()) {
            repairStartResult = Result.error(repairValidationError.get());
        } else if (!isReattachSupportedOnProvider(stack.getStack(), reattach)) {
            repairStartResult = Result.error(RepairValidation
                    .of(String.format("Volume reattach currently not supported on %s platform!", stack.getPlatformVariant())));
        } else {
            Pair<Predicate<HostGroup>, Predicate<InstanceMetaData>> instanceSelectors = getInstanceSelectors(repairMode, selectedParts);
            Map<HostGroupName, Set<InstanceMetaData>> repairableNodes = selectRepairableNodes(instanceSelectors, stack.getStack());
            if (repairableNodes.isEmpty()) {
                repairStartResult = Result.error(RepairValidation.of("Repairable node list is empty. Please check node statuses and try again."));
            } else {
                RepairValidation validationBySelectedNodes = validateSelectedNodes(stack, repairableNodes, reattach);
                if (!validationBySelectedNodes.getValidationErrors().isEmpty()) {
                    repairStartResult = Result.error(validationBySelectedNodes);
                } else {
                    // TODO: it should not be here... we should move it to the repair flow.
                    setStackStatusAndMarkDeletableVolumes(repairMode, deleteVolumes, stack.getStack(), repairableNodes.values().stream()
                            .flatMap(Collection::stream).map(instanceMetaData -> (InstanceMetadataView) instanceMetaData).collect(Collectors.toList()));
                    repairStartResult = Result.success(repairableNodes);
                }
            }
        }
        return repairStartResult;
    }

    public Optional<RepairValidation> validateRepairConditions(ManualClusterRepairMode repairMode, StackDto stack, Set<String> selectedParts) {
        if (!freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), stack.getName())) {
            return Optional.of(RepairValidation.of("Action cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state."));
        } else if (!environmentService.environmentStatusInDesiredState(stack.getStack(), Set.of(EnvironmentStatus.AVAILABLE))) {
            return Optional.of(RepairValidation.of("Action cannot be performed because the Environment isn't available. Please check the Environment state."));
        } else if (hasNotAvailableDatabase(stack)) {
            return Optional.of(RepairValidation.of(String.format("Database %s is not in AVAILABLE status, could not start node replacement.",
                    stack.getCluster().getDatabaseServerCrn())));
        } else if (isHAClusterAndRepairNotAllowed(stack)) {
            return Optional.of(RepairValidation.of("Repair is not supported when the cluster uses cluster proxy and has multiple gateway nodes. " +
                    "This will be fixed in future releases."));
        } else if (isAnyGWUnhealthyAndItIsNotSelected(repairMode, selectedParts, stack)) {
            return Optional.of(RepairValidation.of(String.format("List of unhealthy gateway nodes %s. " +
                    "Gateway nodes must be repaired first.", findAllUnhealthyGatewayNodes(stack)
                    .stream().map(InstanceMetadataView::getInstanceId).collect(toSet()))));
        } else if (isCMRepairAndAllStoppedNodesNotSelected(stack.getNotTerminatedInstanceMetaData(), selectedParts)) {
            return Optional.of(RepairValidation.of("Need to select all stopped nodes as CM node is selected for repair."));
        } else if (!isAllGatewaySelectedWhenSaltVersionIsOutdated(repairMode, selectedParts, stack)) {
            return Optional.of(RepairValidation.of("Gateway node(s) has outdated Salt version. Please include gateway node(s) in the repair selection!"));
        } else {
            return Optional.empty();
        }
    }

    private boolean isAllGatewaySelectedWhenSaltVersionIsOutdated(ManualClusterRepairMode repairMode, Set<String> selectedParts, StackDto stack) {
        Set<String> gatewayInstancesWithOutdatedSaltVersion = saltVersionUpgradeService.getGatewayInstancesWithOutdatedSaltVersion(stack);
        if (!selectedParts.isEmpty() && !gatewayInstancesWithOutdatedSaltVersion.isEmpty()) {
            if (ManualClusterRepairMode.HOST_GROUP.equals(repairMode)) {
                return selectedParts.contains(stack.getPrimaryGatewayGroup().getGroupName());
            } else if (ManualClusterRepairMode.NODE_ID.equals(repairMode)) {
                return selectedParts.containsAll(gatewayInstancesWithOutdatedSaltVersion);
            }
        }
        return true;
    }

    private boolean isCMRepairAndAllStoppedNodesNotSelected(List<InstanceMetadataView> nonTerminatedInstanceMetadata, Set<String> selectedInstances) {
        boolean cmNodeSelectedForRepair = nonTerminatedInstanceMetadata.stream().filter(i -> i.getInstanceId() != null)
                .anyMatch(i -> selectedInstances.contains(i.getInstanceId()) && i.getClusterManagerServer());
        if (cmNodeSelectedForRepair) {
            for (InstanceMetadataView instanceMetadataView : nonTerminatedInstanceMetadata) {
                if (instanceMetadataView.getInstanceStatus().equals(InstanceStatus.STOPPED) &&
                        !selectedInstances.contains(instanceMetadataView.getInstanceId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<InstanceMetadataView> findAllUnhealthyGatewayNodes(StackDto stack) {
        return stack.getNotTerminatedGatewayInstanceMetadata().stream()
                .filter(instanceMetaData -> !instanceMetaData.isHealthy())
                .collect(toList());
    }

    private boolean isAnyGWUnhealthyAndItIsNotSelected(ManualClusterRepairMode repairMode, Set<String> selectedParts, StackDto stack) {
        List<InstanceMetadataView> unhealthyGWs = findAllUnhealthyGatewayNodes(stack);

        if (ManualClusterRepairMode.HOST_GROUP.equals(repairMode)) {
            LOGGER.info("Host group based repair mode, so GW hostgroup should be selected if any GW is not healthy. Unhealthy GWs: {}. Selected instances: {}",
                    unhealthyGWs.stream().map(InstanceMetadataView::getInstanceId).collect(toSet()), selectedParts);
            return unhealthyGWs.stream().anyMatch(unhealthyGW -> !selectedParts.contains(unhealthyGW.getInstanceGroupName()));
        } else if (ManualClusterRepairMode.NODE_ID.equals(repairMode)) {
            LOGGER.info("Node id based repair mode, so GW instance should be selected if it is not healthy. Unhealthy GWs: {}. Selected hostgroups: {}",
                    unhealthyGWs.stream().map(InstanceMetadataView::getInstanceId).collect(toSet()), selectedParts);
            return unhealthyGWs.stream().anyMatch(unhealthyGW -> !selectedParts.contains(unhealthyGW.getInstanceId()));
        } else {
            LOGGER.info("Repair mode is not host group or node id based: {}", repairMode);
            return false;
        }
    }

    private boolean isHAClusterAndRepairNotAllowed(StackDto stack) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return !entitlementService.haRepairEnabled(accountId)
                && stack.getTunnel().useClusterProxy()
                && hasMultipleGatewayInstances(stack);
    }

    private boolean hasMultipleGatewayInstances(StackDto stack) {
        int gatewayInstanceCount = 0;
        for (InstanceGroupDto instanceGroup : stack.getInstanceGroupDtos()) {
            if (InstanceGroupType.isGateway(instanceGroup.getInstanceGroup().getInstanceGroupType())) {
                gatewayInstanceCount += instanceGroup.getNodeCount();
            }
        }
        return gatewayInstanceCount > 1;
    }

    private boolean hasNotAvailableDatabase(StackDto stack) {
        String databaseServerCrn = stack.getCluster().getDatabaseServerCrn();
        if (StringUtils.isNotBlank(databaseServerCrn)) {
            DatabaseServerV4Response databaseServerResponse = redbeamsClientService.getByCrn(databaseServerCrn);
            if (!databaseServerResponse.getStatus().isAvailable()) {
                return true;
            }
        }
        return false;
    }

    private boolean isReattachSupportedOnProvider(StackView stack, boolean repairWithReattach) {
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
        return !instanceMetaData.isHealthy();
    }

    private Map<HostGroupName, Set<InstanceMetaData>> selectRepairableNodes(
            Pair<Predicate<HostGroup>, Predicate<InstanceMetaData>> instanceSelectors,
            StackView stack) {
        return hostGroupService.getByCluster(stack.getClusterId())
                .stream()
                .filter(hostGroup -> RecoveryMode.MANUAL.equals(hostGroup.getRecoveryMode()))
                .filter(instanceSelectors.getLeft())
                .map(hostGroup -> Map.entry(hostGroupName(hostGroup.getName()), hostGroup
                        .getInstanceGroup()
                        .getNotTerminatedAndNotZombieInstanceMetaDataSet()
                        .stream()
                        .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                        .filter(instanceSelectors.getRight())
                        .collect(toSet())))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private RepairValidation validateSelectedNodes(StackDto stack, Map<HostGroupName, Set<InstanceMetaData>> nodesToRepair, boolean reattach) {
        return new RepairValidation(nodesToRepair
                .entrySet()
                .stream()
                .map(entry -> validateRepairableNodes(stack, entry.getKey(), entry.getValue(), reattach))
                .flatMap(Collection::stream)
                .collect(toList())
        );
    }

    private List<String> validateRepairableNodes(StackDto stack, HostGroupName hostGroupName, Set<InstanceMetaData> instances, boolean reattach) {
        List<String> validationResult = new ArrayList<>();
        if (reattach) {
            for (InstanceMetaData instanceMetaData : instances) {
                validationResult.addAll(validateOnGateway(stack, instanceMetaData));
            }
            if (stackStopRestrictionService.isInfrastructureStoppable(stack) != StopRestrictionReason.NONE) {
                validationResult.add("Reattach not supported for this disk type.");
            }
        }
        return validationResult;
    }

    private List<String> validateOnGateway(StackDto stack, InstanceMetadataView instanceMetaData) {
        List<String> validationResult = new ArrayList<>();
        if (instanceMetaData.isGatewayOrPrimaryGateway()) {
            if (isCreatedFromBaseImage(stack.getStack())) {
                validationResult.add("Action is only supported if the image already contains Cloudera Manager and Cloudera Data Platform artifacts.");
            }
            if (!clusterDBValidationService.isGatewayRepairEnabled(stack.getCluster())) {
                validationResult.add(
                        "Action is only supported if Cloudera Manager state is stored in external Database or the cluster was launched after Mar/16/21.");
            }
        }
        return validationResult;
    }

    private boolean isCreatedFromBaseImage(StackView stack) {
        Image modelImage = getImageFromDatabase(stack);
        try {
            return !imageCatalogService.getImage(stack.getWorkspaceId(), modelImage.getImageCatalogUrl(), modelImage.getImageCatalogName(),
                    modelImage.getImageId()).getImage().isPrewarmed();
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn("Image not found with id: {} in catalog: {}", modelImage.getImageId(), modelImage.getImageCatalogUrl());
            return !(modelImage.getPackageVersions() != null && modelImage.getPackageVersions().containsKey(ImagePackageVersion.STACK.getKey()));
        }
    }

    private Image getImageFromDatabase(StackView stack) {
        try {
            return componentConfigProviderService.getImage(stack.getId());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.debug("Image not found in database.");
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private void updateVolumesDeleteFlag(StackView stack, Set<String> instanceIds, Set<String> instanceFQDNs, boolean deleteVolumes) {
        List<Resource> volumes = resourceService.findByStackIdAndType(stack.getId(), stack.getDiskResourceType());
        volumes = volumes.stream()
                .filter(volume -> instanceIds.contains(volume.getInstanceId()) || volumeFQDNIsInInstanceFQDNSet(volume, instanceFQDNs))
                .map(volumeSet -> updateDeleteVolumesFlag(deleteVolumes, volumeSet))
                .collect(toList());
        List<String> volumeNames = volumes.stream().map(Resource::getResourceName).collect(toList());
        LOGGER.info("Update delete volume flag on {} to {}", volumeNames, deleteVolumes);
        resourceService.saveAll(volumes);
    }

    private boolean volumeFQDNIsInInstanceFQDNSet(Resource volume, Set<String> instanceFQDNs) {
        try {
            Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volume, VolumeSetAttributes.class);
            if (attributes.isPresent()) {
                if (instanceFQDNs.contains(attributes.get().getDiscoveryFQDN())) {
                    return true;
                }
            }
        } catch (CloudbreakServiceException cloudbreakServiceException) {
            LOGGER.warn("Can't parse resource attribute into VolumeSetAttributes class", cloudbreakServiceException);
        }
        return false;
    }

    private Resource updateDeleteVolumesFlag(boolean deleteVolumes, Resource volumeSet) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
        attributes.ifPresent(volumeSetAttributes -> {
            volumeSetAttributes.setDeleteOnTermination(deleteVolumes);
            resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
        });
        return volumeSet;
    }

    public void markVolumesToNonDeletable(StackView stack, List<InstanceMetadataView> instanceMetadataViews) {
        updateVolumesDeleteFlag(stack, instanceMetadataViews, false);
    }

    public void setStackStatusAndMarkDeletableVolumes(ManualClusterRepairMode repairMode, boolean deleteVolumes, StackView stack,
            List<InstanceMetadataView> instances) {
        if (!ManualClusterRepairMode.DRY_RUN.equals(repairMode) && !instances.isEmpty()) {
            LOGGER.info("Repair mode is not a dry run, {}", repairMode);
            updateVolumesDeleteFlag(stack, instances, deleteVolumes);
            LOGGER.info("Update stack status to REPAIR_IN_PROGRESS");
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REPAIR_IN_PROGRESS);
        }
    }

    private void updateVolumesDeleteFlag(StackView stack, List<InstanceMetadataView> instanceMetadataViews, boolean deleteVolumes) {
        Set<String> instanceIds = instanceMetadataViews.stream().map(InstanceMetadataView::getInstanceId).collect(toSet());
        Set<String> instanceFQDNs = instanceMetadataViews.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(toSet());
        updateVolumesDeleteFlag(stack, instanceIds, instanceFQDNs, deleteVolumes);
    }

    private FlowIdentifier triggerRepairOrThrowBadRequest(Long stackId, Result<Map<HostGroupName, Set<InstanceMetaData>>,
            RepairValidation> repairValidationResult, RepairType repairType, boolean restartServices, Set<String> recoveryMessageArgument,
            String upgradeVariant, boolean upgrade) {
        if (repairValidationResult.isError()) {
            eventService.fireCloudbreakEvent(stackId, RECOVERY_FAILED, CLUSTER_MANUALRECOVERY_COULD_NOT_START,
                    repairValidationResult.getError().getValidationErrors());
            throw new BadRequestException(String.join(" ", repairValidationResult.getError().getValidationErrors()));
        } else {
            if (!repairValidationResult.getSuccess().isEmpty()) {
                FlowIdentifier flowIdentifier = flowManager.triggerClusterRepairFlow(stackId, toStringMap(repairValidationResult.getSuccess()),
                        repairType, restartServices, upgradeVariant, upgrade);
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
                                .filter(i -> i.getDiscoveryFQDN() != null)
                                .map(InstanceMetaData::getDiscoveryFQDN)
                                .collect(toList())));
    }
}
