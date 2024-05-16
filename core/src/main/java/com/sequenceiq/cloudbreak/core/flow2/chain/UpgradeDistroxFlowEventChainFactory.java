package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType.ALL_AT_ONCE;
import static com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType.BATCH;
import static com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType.ONE_BY_ONE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.EmbeddedDbUpgradeFlowTriggersFactory;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeDistroxFlowEventChainFactory implements FlowEventChainFactory<DistroXUpgradeTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDistroxFlowEventChainFactory.class);

    @Value("${cb.upgrade.validation.distrox.enabled}")
    private boolean upgradeValidationEnabled;

    @Value("${cb.upgrade.batch.repair.enabled:true}")
    private boolean batchRepairEnabled;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private CentosToRedHatUpgradeAvailabilityService centOSToRedHatUpgradeAvailabilityService;

    @Inject
    private EmbeddedDbUpgradeFlowTriggersFactory embeddedDbUpgradeFlowTriggersFactory;

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
        Optional<Image> helperImage = centOSToRedHatUpgradeAvailabilityService.findHelperImageIfNecessary(event.getImageChangeDto().getImageId(),
                event.getResourceId());
        DistroXUpgradeTriggerEvent eventForRuntimeUpgrade = helperImage.map(image -> createEventForRuntimeUpgrade(image, event)).orElse(event);

        LOGGER.debug("Creating flow trigger event queue for distrox upgrade with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        addUpgradeValidationTriggerEvent(eventForRuntimeUpgrade).ifPresent(flowEventChain::add);
        addClusterUpgradePreparationTriggerEvent(eventForRuntimeUpgrade).ifPresent(flowEventChain::add);
        addClusterScaleTriggerEventIfNeeded(event.getResourceId()).ifPresent(flowEventChain::add);
        addSaltUpdateTriggerEvent(eventForRuntimeUpgrade).ifPresent(flowEventChain::add);
        addClusterUpgradeTriggerEvent(eventForRuntimeUpgrade).ifPresent(flowEventChain::add);
        addImageUpdateTriggerEvent(event).ifPresent(flowEventChain::add);
        flowEventChain.addAll(embeddedDbUpgradeFlowTriggersFactory.createFlowTriggers(event.getResourceId(), true));
        addClusterRepairTriggerEventIfNecessary(event).ifPresent(flowEventChain::add);
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private Optional<StopStartUpscaleTriggerEvent> addClusterScaleTriggerEventIfNeeded(Long stackId) {
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
            return Optional.empty();
        } else {
            LOGGER.info("There are {} stopped nodes present. Starting them before the upgrade.", stoppedInstances.size());
            return Optional.of(new StopStartUpscaleTriggerEvent(
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

    private Optional<ClusterUpgradeValidationTriggerEvent> addUpgradeValidationTriggerEvent(DistroXUpgradeTriggerEvent event) {
        if (upgradeValidationEnabled) {
            LOGGER.info("Upgrade validation enabled, adding to flowchain");
            return Optional.of(new ClusterUpgradeValidationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto().getImageId(),
                    event.isLockComponents(), event.isRollingUpgradeEnabled(), event.isReplaceVms()));
        } else {
            LOGGER.info("Upgrade validation disabled");
            return Optional.empty();
        }
    }

    private Optional<ClusterUpgradePreparationTriggerEvent> addClusterUpgradePreparationTriggerEvent(DistroXUpgradeTriggerEvent event) {
        if (event.isLockComponents()) {
            LOGGER.debug("Skip upgrade preparation because the component versions are not changing.");
            return Optional.empty();
        } else {
            return Optional.of(new ClusterUpgradePreparationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto(),
                    event.getRuntimeVersion()));
        }
    }

    private Optional<StackEvent> addSaltUpdateTriggerEvent(DistroXUpgradeTriggerEvent event) {
        return Optional.of(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
    }

    private Optional<ClusterUpgradeTriggerEvent> addClusterUpgradeTriggerEvent(DistroXUpgradeTriggerEvent event) {
        if (event.isLockComponents()) {
            LOGGER.debug("Skip runtime upgrade because the component versions are not changing.");
            return Optional.empty();
        } else {
            return Optional.of(new ClusterUpgradeTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted(),
                    event.getImageChangeDto().getImageId(), event.isRollingUpgradeEnabled()));
        }
    }

    private Optional<StackImageUpdateTriggerEvent> addImageUpdateTriggerEvent(DistroXUpgradeTriggerEvent event) {
        return Optional.of(new StackImageUpdateTriggerEvent(STACK_IMAGE_UPDATE_TRIGGER_EVENT, event.getImageChangeDto()));
    }

    private Optional<ClusterRepairTriggerEvent> addClusterRepairTriggerEventIfNecessary(DistroXUpgradeTriggerEvent event) {
        if (event.isReplaceVms()) {
            Map<String, List<String>> nodeMap = getReplaceableInstancesByHostGroup(event);
            ClusterRepairTriggerEvent.RepairType repairType = decideRepairType(event, nodeMap);
            LOGGER.info("Repair type: {}", repairType);
            return Optional.of(new ClusterRepairTriggerEvent(CLUSTER_REPAIR_TRIGGER_EVENT, event.getResourceId(),
                    repairType, nodeMap, true, event.getTriggeredStackVariant(), event.isRollingUpgradeEnabled()));
        } else {
            return Optional.empty();
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

    private Map<String, List<String>> getReplaceableInstancesByHostGroup(DistroXUpgradeTriggerEvent event) {
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> validationResult = clusterRepairService.validateRepair(ManualClusterRepairMode.ALL,
                event.getResourceId(), Set.of(), false);
        return toStringMap(validationResult.getSuccess());
    }

    private Map<String, List<String>> toStringMap(Map<HostGroupName, Set<InstanceMetaData>> repairableNodes) {
        return repairableNodes
                .entrySet()
                .stream()
                .collect(toMap(entry -> entry.getKey().value(),
                        entry -> entry.getValue()
                                .stream()
                                .map(InstanceMetaData::getDiscoveryFQDN)
                                .filter(Objects::nonNull)
                                .collect(toList())));
    }
}