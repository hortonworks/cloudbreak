package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.OSUpgradeByUpgradeSetsTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class OSUpgradeByUpgradeSetsFlowEventChainFactory implements FlowEventChainFactory<OSUpgradeByUpgradeSetsTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSUpgradeByUpgradeSetsFlowEventChainFactory.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.OS_UPGRADE_BY_UPGRADE_SETS_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(OSUpgradeByUpgradeSetsTriggerEvent event) {
        List<OrderedOSUpgradeSet> upgradeSets = event.getUpgradeSets();
        LOGGER.info("Create flow trigger event queue for upgrade set: {}", upgradeSets);
        Set<String> instanceIdsToRepair = upgradeSets.stream().flatMap(upgradeSet -> upgradeSet.getInstanceIds().stream()).collect(Collectors.toSet());
        LOGGER.info("Instance ids to repair: {}", instanceIdsToRepair);
        Queue<Selectable> flowTriggers = createFlowTriggerList(event, upgradeSets);

        return new FlowTriggerEventQueue(getName(), event, flowTriggers);
    }

    private Queue<Selectable> createFlowTriggerList(OSUpgradeByUpgradeSetsTriggerEvent event, List<OrderedOSUpgradeSet> upgradeSets) {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        StackView stackView = stackDtoService.getStackViewById(event.getResourceId());
        LOGGER.info("Add stack image update trigger event with image DTO: {}", event.getImageChangeDto());
        flowTriggers.add(new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), event.getResourceId(), event.accepted(),
                event.getImageChangeDto().getImageId(), event.getImageChangeDto().getImageCatalogName(), event.getImageChangeDto().getImageCatalogUrl()));
        upgradeSets.stream().sorted(Comparator.comparingInt(OrderedOSUpgradeSet::getOrder)).forEach(upgradeSet -> {
            Set<String> instanceIdsToUpgradeAtOnce = upgradeSet.getInstanceIds();
            LOGGER.info("Instance set with order {}: {}", upgradeSet.getOrder(), instanceIdsToUpgradeAtOnce);
            List<InstanceMetadataView> instancesToUpgradeAtOnce = instanceMetaDataService.findAllViewByStackIdAndInstanceId(event.getResourceId(),
                    instanceIdsToUpgradeAtOnce);
            Map<String, Set<String>> groupsWithHostNames = instancesToUpgradeAtOnce.stream().collect(groupingBy(InstanceMetadataView::getInstanceGroupName,
                    Collectors.mapping(InstanceMetadataView::getDiscoveryFQDN, Collectors.toSet())));
            Map<String, Set<Long>> groupsWithPrivateIds = instancesToUpgradeAtOnce.stream().collect(groupingBy(InstanceMetadataView::getInstanceGroupName,
                    Collectors.mapping(InstanceMetadataView::getPrivateId, Collectors.toSet())));
            Map<String, Integer> groupsWithAdjustment = instancesToUpgradeAtOnce.stream().collect(Collectors.toMap(InstanceMetadataView::getInstanceGroupName,
                    instanceMetadataView -> groupsWithHostNames.get(instanceMetadataView.getInstanceGroupName()).size(), (key1, key2) -> key2));
            boolean repairingPrimaryGW = isRepairingPrimaryGW(event.getResourceId(), instanceIdsToUpgradeAtOnce);
            clusterRepairService.markVolumesToNonDeletable(stackView, instancesToUpgradeAtOnce);
            addDownscaleFlowTrigger(event, flowTriggers, repairingPrimaryGW, groupsWithHostNames, groupsWithPrivateIds, groupsWithAdjustment);
            // maybe AWS migration should be implemented here later
            addUpscaleFlowTrigger(event, flowTriggers, upgradeSets, repairingPrimaryGW, groupsWithHostNames, groupsWithAdjustment);
        });
        return flowTriggers;
    }

    private boolean isRepairingPrimaryGW(Long stackId, Set<String> instanceIdsToUpgradeAtOnce) {
        boolean repairingPrimaryGW = false;
        Optional<InstanceMetadataView> primaryGatewayInstanceMetadata = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stackId);
        if (primaryGatewayInstanceMetadata.isPresent()) {
            String primaryGWInstanceId = primaryGatewayInstanceMetadata.get().getInstanceId();
            LOGGER.info("Primary GW: {}", primaryGWInstanceId);
            repairingPrimaryGW = instanceIdsToUpgradeAtOnce.contains(primaryGWInstanceId);
        }
        LOGGER.info("Repairing primary GW: {}", repairingPrimaryGW);
        return repairingPrimaryGW;
    }

    private void addDownscaleFlowTrigger(OSUpgradeByUpgradeSetsTriggerEvent event, Queue<Selectable> flowTriggers, boolean repairingPrimaryGW,
            Map<String, Set<String>> groupsWithHostNames, Map<String, Set<Long>> groupsWithPrivateIds, Map<String, Integer> groupsWithAdjustment) {
        LOGGER.info("Add downscale flow trigger for: {}", groupsWithHostNames);
        if (repairingPrimaryGW) {
            LOGGER.info("Primary GW is in repaired host list, cluster downscale should be skipped");
            flowTriggers.add(new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), groupsWithAdjustment, groupsWithPrivateIds,
                    groupsWithHostNames, event.getPlatformVariant(), event.accepted()).setRepair());
        } else {
            flowTriggers.add(new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getResourceId(),
                groupsWithAdjustment, groupsWithPrivateIds, groupsWithHostNames, ScalingType.DOWNSCALE_TOGETHER, event.accepted(),
                    new ClusterDownscaleDetails(true, true, false)));
        }
    }

    private void addUpscaleFlowTrigger(OSUpgradeByUpgradeSetsTriggerEvent event, Queue<Selectable> flowTriggers, List<OrderedOSUpgradeSet> upgradeSets,
            boolean repairingPrimaryGW, Map<String, Set<String>> groupsWithHostNames, Map<String, Integer> groupsWithAdjustment) {
        LOGGER.info("Add upscale flow trigger for: {}", groupsWithHostNames);
        StackView stackView = stackDtoService.getStackViewById(event.getResourceId());
        boolean singleNodeCluster = upgradeSets.stream().mapToLong(numberedOsUpgradeSet -> numberedOsUpgradeSet.getInstanceIds().size()).sum() == 1;

        LOGGER.info("Primary GW will be repaired: {}", repairingPrimaryGW);
        flowTriggers.add(new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getResourceId(),
                groupsWithAdjustment, null, groupsWithHostNames, ScalingType.UPSCALE_TOGETHER, repairingPrimaryGW,
                isKerberosSecured(stackView), event.accepted(),
                singleNodeCluster,
                false,
                ClusterManagerType.CLOUDERA_MANAGER,
                new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, groupsWithAdjustment.values().stream().mapToLong(Integer::intValue).sum()),
                event.getPlatformVariant(),
                true).setRepair());
    }

    private boolean isKerberosSecured(StackView stackView) {
        boolean kerberosConfigExistsForEnvironment = kerberosConfigService.isKerberosConfigExistsForEnvironment(stackView.getEnvironmentCrn(),
                stackView.getName());
        LOGGER.info("Kerberos config exists for env: {}", kerberosConfigExistsForEnvironment);
        return kerberosConfigExistsForEnvironment;
    }

}
