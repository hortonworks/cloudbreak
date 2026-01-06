package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode.ALL;
import static com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode.NODE_ID;
import static com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType.ALL_AT_ONCE;
import static com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType.BATCH;
import static com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType.ONE_BY_ONE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.chain.util.SetDefaultJavaVersionFlowChainService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.EmbeddedDbUpgradeFlowTriggersFactory;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUtil;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.ClusterSizeUpgradeValidator;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeDistroxFlowEventChainFactory implements FlowEventChainFactory<DistroXUpgradeTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDistroxFlowEventChainFactory.class);

    @Value("${cb.upgrade.batch.repair.enabled:true}")
    private boolean batchRepairEnabled;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private OsChangeUtil osChangeUtil;

    @Inject
    private EmbeddedDbUpgradeFlowTriggersFactory embeddedDbUpgradeFlowTriggersFactory;

    @Inject
    private SaltVersionUpgradeService saltVersionUpgradeService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterSizeUpgradeValidator clusterSizeUpgradeValidator;

    @Inject
    private SetDefaultJavaVersionFlowChainService setDefaultJavaVersionFlowChainService;

    @Override
    public String initEvent() {
        return DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public CDPClusterStatus.Value getUseCaseForFlowState(Enum flowState) {
        if (SaltUpdateState.INIT_STATE.equals(flowState)) {
            return UPGRADE_STARTED;
        } else if (ClusterUpgradeState.CLUSTER_UPGRADE_FINISHED_STATE.equals(flowState)) {
            return UPGRADE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE") &&
                !ClusterDownscaleState.DECOMISSION_FAILED_STATE.equals(flowState) &&
                !ClusterDownscaleState.REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_STATE.equals(flowState)) {
            return UPGRADE_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DistroXUpgradeTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for data hub upgrade with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        DistroXUpgradeTriggerEvent eventForRuntimeUpgrade = getEventForRuntimeUpgrade(event);

        StackDto stack = stackDtoService.getByIdWithoutResources(event.getResourceId());

        flowEventChain.addAll(getFullSyncEvent(event));
        flowEventChain.addAll(getUpgradeValidationTriggerEvent(eventForRuntimeUpgrade));
        flowEventChain.addAll(getClusterUpgradePreparationTriggerEvent(eventForRuntimeUpgrade));
        flowEventChain.addAll(getClusterScaleTriggerEvent(event.getResourceId()));
        flowEventChain.addAll(saltVersionUpgradeService.getSaltSecretRotationTriggerEvent(event.getResourceId()));
        flowEventChain.addAll(getSaltUpdateTriggerEvent(eventForRuntimeUpgrade));
        flowEventChain.addAll(getClusterUpgradeTriggerEvent(eventForRuntimeUpgrade, stack));
        flowEventChain.addAll(getImageUpdateTriggerEvent(event));
        flowEventChain.addAll(embeddedDbUpgradeFlowTriggersFactory.createFlowTriggers(event.getResourceId(), true));
        flowEventChain.addAll(getClusterRepairTriggerEvent(event, stack));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private DistroXUpgradeTriggerEvent getEventForRuntimeUpgrade(DistroXUpgradeTriggerEvent event) {
        return osChangeUtil.findHelperImageIfNecessary(
                        event.getImageChangeDto().getImageId(),
                        event.getResourceId())
                .map(image -> createEventForRuntimeUpgrade(image, event))
                .orElse(event);
    }

    private List<StopStartUpscaleTriggerEvent> getClusterScaleTriggerEvent(Long stackId) {
        String hostGroup = "";
        List<String> stoppedInstances = new ArrayList<>();
        for (InstanceMetadataView instanceMetadataView : instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stackId)) {
            if (instanceMetadataView.getInstanceStatus().equals(InstanceStatus.STOPPED)) {
                stoppedInstances.add(instanceMetadataView.getInstanceId());
                hostGroup = instanceMetadataView.getInstanceGroupName();
            }
        }

        if (stoppedInstances.isEmpty()) {
            LOGGER.info("There are no stopped instances present.");
            return List.of();
        } else {
            LOGGER.info("There are {} stopped nodes present. Starting them before the upgrade.", stoppedInstances.size());
            return List.of(new StopStartUpscaleTriggerEvent(
                    StopStartUpscaleEvent.STOPSTART_UPSCALE_TRIGGER_EVENT.event(),
                    stackId,
                    hostGroup,
                    stoppedInstances.size(),
                    ClusterManagerType.CLOUDERA_MANAGER,
                    false));
        }
    }

    private DistroXUpgradeTriggerEvent createEventForRuntimeUpgrade(Image helperImage, DistroXUpgradeTriggerEvent event) {
        ImageChangeDto imageChangeDto = event.getImageChangeDto();
        LOGGER.debug("Creating new event where changing the image from RHEL8 {} to centos7 {} for perform the runtime upgrade", imageChangeDto.getImageId(),
                helperImage.getUuid());
        return new DistroXUpgradeTriggerEvent(event.getSelector(), event.getResourceId(), event.accepted(),
                new ImageChangeDto(event.getResourceId(), helperImage.getUuid(), imageChangeDto.getImageCatalogName(), imageChangeDto.getImageCatalogUrl()),
                event.isReplaceVms(), event.isLockComponents(), event.getTriggeredStackVariant(), event.isRollingUpgradeEnabled(), event.getRuntimeVersion());
    }

    private List<ClusterUpgradeValidationTriggerEvent> getUpgradeValidationTriggerEvent(DistroXUpgradeTriggerEvent event) {
        LOGGER.info("Upgrade validation enabled, adding to flowchain");
        return List.of(
                new ClusterUpgradeValidationTriggerEvent(
                        event.getResourceId(),
                        event.accepted(),
                        event.getImageChangeDto().getImageId(),
                        event.isLockComponents(),
                        event.isRollingUpgradeEnabled(),
                        event.isReplaceVms()
                )
        );
    }

    private List<Selectable> getFullSyncEvent(DistroXUpgradeTriggerEvent event) {
        LOGGER.info("Add sync events for full sync");
        List<Selectable> syncEvents = new ArrayList<>();

        syncEvents.add(new StackSyncTriggerEvent(STACK_SYNC_EVENT.event(), event.getResourceId(), true, event.accepted()));
        syncEvents.add(new StackEvent(CLUSTER_SYNC_EVENT.event(), event.getResourceId()));

        return syncEvents;
    }

    private List<ClusterUpgradePreparationTriggerEvent> getClusterUpgradePreparationTriggerEvent(DistroXUpgradeTriggerEvent event) {
        if (event.isLockComponents()) {
            LOGGER.debug("Skip upgrade preparation because the component versions are not changing.");
            return List.of();
        } else {
            return List.of(new ClusterUpgradePreparationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto(),
                    event.getRuntimeVersion()));
        }
    }

    private List<StackEvent> getSaltUpdateTriggerEvent(DistroXUpgradeTriggerEvent event) {
        return List.of(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
    }

    private List<Selectable> getClusterUpgradeTriggerEvent(DistroXUpgradeTriggerEvent event, StackDto stack) {
        if (event.isLockComponents()) {
            LOGGER.debug("Skip runtime upgrade because the component versions are not changing.");
            return List.of();
        } else {
            List<Selectable> upgradeEvents =
                    new ArrayList<>(setDefaultJavaVersionFlowChainService.setDefaultJavaVersionTriggerEvent(stack, event.getImageChangeDto()));
            upgradeEvents.add(new ClusterUpgradeTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted(),
                    event.getImageChangeDto().getImageId(), event.isRollingUpgradeEnabled()));
            return upgradeEvents;
        }
    }

    private List<StackImageUpdateTriggerEvent> getImageUpdateTriggerEvent(DistroXUpgradeTriggerEvent event) {
        if (event.isLockComponents() && !event.isReplaceVms()) {
            LOGGER.warn("OS upgrade without replacing the instances (lockComponents=true, replaceVms=false) is invalid, do not update image.");
            return List.of();
        } else {
            return List.of(new StackImageUpdateTriggerEvent(STACK_IMAGE_UPDATE_TRIGGER_EVENT, event.getImageChangeDto()));
        }
    }

    private List<ClusterRepairTriggerEvent> getClusterRepairTriggerEvent(DistroXUpgradeTriggerEvent event, StackDto stack) {
        LOGGER.info("ReplaceVms: {}, lockComponents: {}", event.isReplaceVms(), event.isLockComponents());
        if (event.isReplaceVms()) {
            Pair<Boolean, Map<String, List<String>>> variantMigrationFeasibleNodeMapPair = getReplaceableInstancesByHostGroup(event, stack);
            if (variantMigrationFeasibleNodeMapPair.getRight().isEmpty()) {
                LOGGER.info("OS upgrade is not required, as replaceable node list is empty.");
                return List.of();
            }
            ClusterRepairTriggerEvent.RepairType repairType = decideRepairType(event, variantMigrationFeasibleNodeMapPair.getRight());
            String triggeredStackVariant = calculateTriggeredStackVariant(event.getTriggeredStackVariant(),
                    variantMigrationFeasibleNodeMapPair.getLeft(), stack);
            LOGGER.info("Repair type: {}", repairType);
            return List.of(new ClusterRepairTriggerEvent(
                    CLUSTER_REPAIR_TRIGGER_EVENT,
                    event.getResourceId(),
                    repairType,
                    variantMigrationFeasibleNodeMapPair.getRight(),
                    true,
                    triggeredStackVariant,
                    event.isRollingUpgradeEnabled()
            ));
        } else {
            LOGGER.info("The replaceVms flag is false, no need to add repair trigger.");
            return List.of();
        }
    }

    private String calculateTriggeredStackVariant(String triggeredStackVariant, boolean variantMigrationFeasible, StackDto stack) {
        if (variantMigrationFeasible) {
            return triggeredStackVariant;
        } else {
            LOGGER.info("Use the original platform variant ({}) for repair to skip variant migration because not all host groups are selected.",
                    stack.getPlatformVariant());
            return stack.getPlatformVariant();
        }
    }

    private ClusterRepairTriggerEvent.RepairType decideRepairType(DistroXUpgradeTriggerEvent event, Map<String, List<String>> nodeMap) {
        long nodeCount = nodeMap.values().stream().mapToLong(java.util.Collection::size).sum();
        int maxUpscaleStepInNodeCount = scalingHardLimitsService.getMaxUpscaleStepInNodeCount();
        LOGGER.info("Batch repair enabled: {}, node count: {}, max upscale step: {}",
                batchRepairEnabled, nodeCount, maxUpscaleStepInNodeCount);
        if (event.isRollingUpgradeEnabled()) {
            return ONE_BY_ONE;
        } else if (batchRepairEnabled && nodeCount > maxUpscaleStepInNodeCount) {
            return BATCH;
        } else {
            return ALL_AT_ONCE;
        }
    }

    private Pair<Boolean, Map<String, List<String>>> getReplaceableInstancesByHostGroup(DistroXUpgradeTriggerEvent event, StackDto stack) {
        if (event.isReplaceVms() && !event.isLockComponents()) {
            LOGGER.info("Force OS upgrade is enabled by entitlement or requested by explicitly specifying replaceVms as true and lockComponents as false.");
            if (replaceMasterNodesOnly(event.isRollingUpgradeEnabled(), stack)) {
                Set<String> gatewayInstanceIds = stack.getAllAvailableGatewayInstances().stream()
                        .map(InstanceMetadataView::getInstanceId)
                        .collect(toSet());
                Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validationResult = clusterRepairService.validateRepair(NODE_ID,
                        event.getResourceId(), gatewayInstanceIds, false);
                return Pair.of(Boolean.FALSE, filterBasedOnImageAndConvertToFqdn(validationResult.getSuccess(), event.getImageChangeDto().getImageId()));
            }
        }
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validationResult = clusterRepairService.validateRepair(ALL,
                event.getResourceId(), Set.of(), false);
        return Pair.of(Boolean.TRUE, filterBasedOnImageAndConvertToFqdn(validationResult.getSuccess(), event.getImageChangeDto().getImageId()));
    }

    private boolean replaceMasterNodesOnly(boolean rollingUpgradeEnabled, StackDto stack) {
        if (rollingUpgradeEnabled && clusterSizeUpgradeValidator.isClusterSizeLargerThanAllowedForRollingUpgrade(stack.getFullNodeCount())) {
            LOGGER.info("This is a rolling upgrade with a cluster size exceeding the allowed limit, replace master nodes only. " +
                    "Upgrade validation may fail if the cluster size is too large (controlled by CDP_SKIP_ROLLING_UPGRADE_VALIDATION flag).");
            return true;
        } else if (CodUtil.isCodCluster(stack)) {
            LOGGER.info("This is a COD cluster, replace master nodes only.");
            return true;
        } else {
            LOGGER.info("This is a regular upgrade, replace all nodes");
            return false;
        }
    }

    private Map<String, List<String>> filterBasedOnImageAndConvertToFqdn(Map<HostGroupName, Set<InstanceMetaData>> repairableNodes, String stackImageId) {
        Map<String, List<String>> repairableFqdns = repairableNodes.entrySet().stream()
                .collect(toMap(entry -> entry.getKey().value(),
                        entry -> entry.getValue().stream()
                                .filter(instanceMetadata -> stackImageId == null || !stackImageId.equals(getImageIdFromInstance(instanceMetadata)))
                                .map(InstanceMetaData::getDiscoveryFQDN)
                                .filter(Objects::nonNull)
                                .collect(toList())));
        return repairableFqdns.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getImageIdFromInstance(InstanceMetadataView instanceMetadata) {
        try {
            if (instanceMetadata != null && instanceMetadata.getImage() != null) {
                return instanceMetadata.getImage().get(com.sequenceiq.cloudbreak.cloud.model.Image.class).getImageId();
            }
        } catch (IOException e) {
            LOGGER.warn("Missing image information for instance: " + instanceMetadata.getInstanceId(), e);
        }
        return null;
    }
}