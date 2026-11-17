package com.sequenceiq.cloudbreak.core.flow2.chain;

import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.LockedComponentsValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.OSUpgradeByUpgradeSetsTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.OrderedOsUpgradeRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class OSUpgradeByUpgradeSetsFlowEventChainFactory implements FlowEventChainFactory<OSUpgradeByUpgradeSetsTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSUpgradeByUpgradeSetsFlowEventChainFactory.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackDtoService stackDtoService;

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
        if (stackView.isDatahub()) {
            flowTriggers.add(new LockedComponentsValidationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto()));
        }
        LOGGER.info("Add stack image update trigger event with image DTO: {}", event.getImageChangeDto());
        flowTriggers.add(new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), event.getResourceId(), event.accepted(),
                event.getImageChangeDto().getImageId(), event.getImageChangeDto().getImageCatalogName(), event.getImageChangeDto().getImageCatalogUrl()));
        upgradeSets.stream().sorted(Comparator.comparingInt(OrderedOSUpgradeSet::getOrder)).forEach(upgradeSet -> {
            Set<String> instanceIdsToUpgradeAtOnce = upgradeSet.getInstanceIds();
            LOGGER.info("Instance set with order {}: {}", upgradeSet.getOrder(), instanceIdsToUpgradeAtOnce);
            List<InstanceMetadataView> instancesToUpgradeAtOnce = instanceMetaDataService.findAllViewByStackIdAndInstanceId(event.getResourceId(),
                    instanceIdsToUpgradeAtOnce);
            Map<String, List<String>> discoveryFQDNsByGroups = instancesToUpgradeAtOnce.stream().collect(groupingBy(InstanceMetadataView::getInstanceGroupName,
                    Collectors.mapping(InstanceMetadataView::getDiscoveryFQDN, Collectors.toList())));
            LOGGER.debug("Triggering upgrade sets with FQDNs by group names: {}", discoveryFQDNsByGroups);
            flowTriggers.add(new OrderedOsUpgradeRepairTriggerEvent(event.getResourceId(), discoveryFQDNsByGroups,
                    event.getPlatformVariant(), instanceIdsToUpgradeAtOnce));
        });
        return flowTriggers;
    }

}
