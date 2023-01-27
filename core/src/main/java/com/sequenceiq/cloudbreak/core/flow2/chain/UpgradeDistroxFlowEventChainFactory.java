package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeDistroxFlowEventChainFactory implements FlowEventChainFactory<DistroXUpgradeTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDistroxFlowEventChainFactory.class);

    @Value("${cb.upgrade.validation.distrox.enabled}")
    private boolean upgradeValidationEnabled;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DistroXUpgradeTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for distrox upgrade with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        createUpgradeValidationEvent(event).ifPresent(flowEventChain::add);
        createClusterUpgradePreparationTriggerEvent(event).ifPresent(flowEventChain::add);
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        createClusterUpgradeTriggerEvent(event).ifPresent(flowEventChain::add);
        flowEventChain.add(new StackImageUpdateTriggerEvent(FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT, event.getImageChangeDto()));
        if (event.isReplaceVms()) {
            Map<String, List<String>> nodeMap = getReplaceableInstancesByHostgroup(event);
            flowEventChain.add(new ClusterRepairTriggerEvent(FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT, event.getResourceId(),
                    event.isRollingUpgradeEnabled(), nodeMap, true, event.getTriggeredStackVariant()));
        }
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
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

    private static Optional<ClusterUpgradeTriggerEvent> createClusterUpgradeTriggerEvent(DistroXUpgradeTriggerEvent event) {
        if (event.isLockComponents()) {
            LOGGER.debug("Skip runtime upgrade because the component versions are not changing.");
            return Optional.empty();
        } else {
            return Optional.of(new ClusterUpgradeTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted(),
                    event.getImageChangeDto().getImageId(), event.isRollingUpgradeEnabled()));
        }
    }

    private Optional<ClusterUpgradeValidationTriggerEvent> createUpgradeValidationEvent(DistroXUpgradeTriggerEvent event) {
        if (upgradeValidationEnabled) {
            LOGGER.info("Upgrade validation enabled, adding to flowchain");
            return Optional.of(new ClusterUpgradeValidationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto().getImageId(),
                    event.isLockComponents()));
        } else {
            LOGGER.info("Upgrade validation disabled");
            return Optional.empty();
        }
    }

    private Optional<ClusterUpgradePreparationTriggerEvent> createClusterUpgradePreparationTriggerEvent(DistroXUpgradeTriggerEvent event) {
        if (event.isLockComponents()) {
            LOGGER.debug("Skip upgrade preparation because the component versions are not changing.");
            return Optional.empty();
        } else {
            return Optional.of(new ClusterUpgradePreparationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto()));
        }
    }

    private Map<String, List<String>> getReplaceableInstancesByHostgroup(DistroXUpgradeTriggerEvent event) {
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
                                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                                .map(InstanceMetaData::getDiscoveryFQDN)
                                .collect(toList())));
    }
}
