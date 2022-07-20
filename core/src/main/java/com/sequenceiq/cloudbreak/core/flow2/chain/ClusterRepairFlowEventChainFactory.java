package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RescheduleStatusCheckTriggerEvent;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class ClusterRepairFlowEventChainFactory implements FlowEventChainFactory<ClusterRepairTriggerEvent> {

    private static final Logger LOGGER = getLogger(ClusterRepairFlowEventChainFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterRepairTriggerEvent event) {
        LOGGER.debug("Creating repair flow chain with stack id: '{}'", event.getStackId());
        StackView stack = stackViewService.getById(event.getStackId());
        RepairConfig repairConfig = createRepairConfig(event, stack);
        Queue<Selectable> flowTriggers = createFlowTriggers(event, repairConfig, stack);
        return new FlowTriggerEventQueue(getName(), event, flowTriggers);
    }

    private RepairConfig createRepairConfig(ClusterRepairTriggerEvent event, StackView stack) {
        RepairConfig repairConfig = new RepairConfig();
        for (Entry<String, List<String>> failedNodes : event.getFailedNodesMap().entrySet()) {
            String hostGroupName = failedNodes.getKey();
            List<String> hostNames = failedNodes.getValue();
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getClusterView().getId(), hostGroupName)
                    .orElseThrow(notFound("hostgroup", hostGroupName));
            InstanceGroup instanceGroup = hostGroup.getInstanceGroup();
            if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                Optional<String> primaryGatewayHostName = instanceMetaDataService.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(event.getStackId(),
                        instanceGroup.getId());
                boolean primaryGatewayRepairable = primaryGatewayHostName.isPresent() && hostNames.contains(primaryGatewayHostName.get());
                if (primaryGatewayRepairable) {
                    repairConfig.setSinglePrimaryGateway(new Repair(instanceGroup.getGroupName(), hostGroup.getName(), hostNames));
                } else {
                    repairConfig.addRepairs(new Repair(instanceGroup.getGroupName(), hostGroup.getName(), hostNames));
                }
            } else {
                repairConfig.addRepairs(new Repair(instanceGroup.getGroupName(), hostGroup.getName(), hostNames));
            }
        }
        return repairConfig;
    }

    private Queue<Selectable> createFlowTriggers(ClusterRepairTriggerEvent event, RepairConfig repairConfig, StackView stackView) {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        flowTriggers.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        Map<String, Set<String>> repairableGroupsWithHostNames = new HashMap<>();
        boolean singlePrimaryGW = fillRepairableGroupsWithHostNames(repairConfig, repairableGroupsWithHostNames);
        LOGGER.info("Repairable groups with host names: {}", repairableGroupsWithHostNames);
        addDownscaleAndUpscaleEvents(event, flowTriggers, repairableGroupsWithHostNames, singlePrimaryGW, stackView);
        flowTriggers.add(rescheduleStatusCheckEvent(event));
        flowTriggers.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return flowTriggers;
    }

    private boolean fillRepairableGroupsWithHostNames(RepairConfig repairConfig, Map<String, Set<String>> repairableGroupsWithHostNames) {
        boolean singlePrimaryGW = addGatewayGroupWithHostNames(repairConfig, repairableGroupsWithHostNames);
        repairableGroupsWithHostNames.putAll(repairConfig.getRepairs().stream().collect(Collectors.toMap(Repair::getHostGroupName,
                repair -> new HashSet<>(repair.getHostNames()))));
        return singlePrimaryGW;
    }

    private void addDownscaleAndUpscaleEvents(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers, Map<String,
            Set<String>> repairableGroupsWithHostNames, boolean singlePrimaryGW, StackView stackView) {
        Crn crnById = Crn.safeFromString(stackView.getResourceCrn());
        if (event.isOneNodeFromEachHostGroupAtOnce() && entitlementService.isDatalakeZduOSUpgradeEnabled(crnById.getAccountId())) {
            LOGGER.info("Rolling upgrade, repairing one node from each host group at one time, for stack: '{}'", event.getStackId());
            repairOneNodeFromEachHostGroupAtOnce(event, flowTriggers, repairableGroupsWithHostNames, stackView);
        } else {
            LOGGER.info("Upgrading all the nodes by groups, upgrading all the nodes within a group at the same time, for stack: '{}'", event.getStackId());
            addRepairFlows(event, flowTriggers, repairableGroupsWithHostNames, singlePrimaryGW, stackView);
        }
    }

    private void repairOneNodeFromEachHostGroupAtOnce(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers,
            Map<String, Set<String>> repairableGroupsWithHostNames, StackView stackView) {
        Optional<String> primaryGwFQDN = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(event.getStackId()).map(InstanceMetaData::getDiscoveryFQDN);
        HashMultimap<String, String> repairableGroupsWithHostNameMultimap = HashMultimap.create();
        repairableGroupsWithHostNames.forEach(repairableGroupsWithHostNameMultimap::putAll);
        LinkedListMultimap<String, String> hostsByHostGroupAndSortedByPgw =
                collectHostsByHostGroupAndSortByPgw(primaryGwFQDN, repairableGroupsWithHostNameMultimap);
        addRepairFlowsForEachGroupsWithOneNode(event, flowTriggers, hostsByHostGroupAndSortedByPgw, primaryGwFQDN, stackView);
    }

    private LinkedListMultimap<String, String> collectHostsByHostGroupAndSortByPgw(Optional<String> primaryGwFQDN,
            HashMultimap<String, String> repairableGroupsWithHostNameMultimap) {
        return repairableGroupsWithHostNameMultimap.entries().stream()
                .sorted(Entry.comparingByValue(Comparator.comparing(s -> primaryGwFQDN.filter(s::equals).isEmpty())))
                .collect(Multimaps.toMultimap(Entry::getKey, Entry::getValue, LinkedListMultimap::create));
    }

    private void addRepairFlowsForEachGroupsWithOneNode(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers,
            Multimap<String, String> orderedHostMultimap, Optional<String> primaryGwFQDNOptional, StackView stackView) {
        while (!orderedHostMultimap.values().isEmpty()) {
            Map<String, Set<String>> repairableGroupsWithOneHostName = new HashMap<>();
            for (String hostGroup : new HashSet<>(orderedHostMultimap.keySet())) {
                orderedHostMultimap.get(hostGroup).stream().findFirst().ifPresent(hostName -> {
                    repairableGroupsWithOneHostName.put(hostGroup, Collections.singleton(hostName));
                    orderedHostMultimap.values().remove(hostName);
                });
            }
            Set<String> repairedHosts = repairableGroupsWithOneHostName.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            addRepairFlows(event, flowTriggers, repairableGroupsWithOneHostName, isPrimaryGWInHosts(primaryGwFQDNOptional, repairedHosts), stackView);
        }
    }

    private boolean isPrimaryGWInHosts(Optional<String> primaryGwFQDN, Collection<String> hostNames) {
        return hostNames.stream().anyMatch(fqdn -> primaryGwFQDN.filter(fqdn::equals).isPresent());
    }

    private void addRepairFlows(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers, Map<String, Set<String>> repairableGroupsWithHostNames,
            boolean singlePrimaryGW, StackView stackView) {
        if (!repairableGroupsWithHostNames.isEmpty()) {
            flowTriggers.add(downscaleEvent(singlePrimaryGW, event, repairableGroupsWithHostNames));
            LOGGER.info("Downscale event added for: {}", repairableGroupsWithHostNames);
            for (Entry<String, Set<String>> groupWithHostNames : repairableGroupsWithHostNames.entrySet()) {
                addAwsNativeEventMigrationIfNeeded(flowTriggers, event, groupWithHostNames.getKey(), stackView);
            }
            flowTriggers.add(fullUpscaleEvent(event, repairableGroupsWithHostNames, singlePrimaryGW,
                    event.isRestartServices(), isKerberosSecured(event.getStackId())));
            LOGGER.info("Upscale event added for: {}", repairableGroupsWithHostNames);
        }
    }

    private boolean addGatewayGroupWithHostNames(RepairConfig repairConfig, Map<String, Set<String>> groupsWithHostNames) {
        if (repairConfig.getSinglePrimaryGateway().isPresent()) {
            LOGGER.info("Single primary GW flag true");
            Repair repair = repairConfig.getSinglePrimaryGateway().get();
            Map<String, Set<String>> gatewayGroupWithHostNames = Collections.singletonMap(repair.getHostGroupName(), new HashSet<>(repair.getHostNames()));
            groupsWithHostNames.putAll(gatewayGroupWithHostNames);
            LOGGER.info("GW group with hostnames are added: {}", gatewayGroupWithHostNames);
            return true;
        } else {
            return false;
        }
    }

    void addAwsNativeEventMigrationIfNeeded(Queue<Selectable> flowTriggers, ClusterRepairTriggerEvent event, String groupName, StackView stackView) {
        String triggeredVariant = event.getTriggeredStackVariant();
        if (event.isUpgrade()) {
            String originalPlatformVariant = stackView.getPlatformVariant();
            LOGGER.debug("Upgrade flow, checking that the variant migration is triggerable from original: '{}' to new: '{}', groupName: '{}'",
                    originalPlatformVariant, triggeredVariant, groupName);
            if (awsVariantMigrationIsFeasible(stackView, triggeredVariant, originalPlatformVariant)) {
                LOGGER.info("Migration variant is needed from '{}' to: '{}', groupName: '{}'", originalPlatformVariant, triggeredVariant, groupName);
                flowTriggers.add(awsVariantMigrationTriggerEvent(event.getResourceId(), groupName));
            }
        } else {
            LOGGER.debug("Don't need to migrate the stack, variant: {}, groupName: {}", triggeredVariant, groupName);
        }
    }

    private boolean awsVariantMigrationIsFeasible(StackView stackView, String triggeredVariant, String originalPlatformVariant) {
        Crn crn = Crn.safeFromString(stackView.getResourceCrn());
        return AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(originalPlatformVariant)
                && AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value().equals(triggeredVariant)
                && entitlementService.awsVariantMigrationEnable(crn.getAccountId());
    }

    private AwsVariantMigrationTriggerEvent awsVariantMigrationTriggerEvent(Long resourceId, String groupName) {
        return new AwsVariantMigrationTriggerEvent(AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event(), resourceId, groupName);
    }

    private StackEvent downscaleEvent(boolean singlePrimaryGW, ClusterRepairTriggerEvent event,
            Map<String, Set<String>> groupsWithHostNames) {
        Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.getAllInstanceMetadataWithoutInstanceGroupByStackId(event.getStackId());
        Map<String, Set<Long>> groupsWithPrivateIds = new HashMap<>();
        Map<String, Integer> groupsWithAdjustment = new HashMap<>();
        for (Entry<String, Set<String>> groupWithHostNames : groupsWithHostNames.entrySet()) {
            Set<String> hostNames = groupWithHostNames.getValue();
            String group = groupWithHostNames.getKey();
            Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(instanceMetaData, hostNames);
            groupsWithPrivateIds.put(group, privateIdsForHostNames);
            int size = hostNames != null ? hostNames.size() : 0;
            groupsWithAdjustment.put(group, size);
        }
        LOGGER.info("Downscale groups with adjustments: {}", groupsWithAdjustment);
        LOGGER.info("Downscale groups with privateIds: {}", groupsWithPrivateIds);
        if (!singlePrimaryGW) {
            LOGGER.info("Full downscale for the following: {}", groupsWithHostNames);
            return new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getResourceId(), groupsWithAdjustment,
                    groupsWithPrivateIds, groupsWithHostNames, ScalingType.DOWNSCALE_TOGETHER, event.accepted(),
                    new ClusterDownscaleDetails(true, true));
        } else {
            LOGGER.info("Stack downscale for the following: {}", groupsWithHostNames);
            return new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), groupsWithAdjustment, groupsWithPrivateIds,
                    groupsWithHostNames, event.getTriggeredStackVariant(), event.accepted()).setRepair();
        }
    }

    private RescheduleStatusCheckTriggerEvent rescheduleStatusCheckEvent(ClusterRepairTriggerEvent event) {
        return new RescheduleStatusCheckTriggerEvent(FlowChainTriggers.RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT,
                event.getResourceId(), event.accepted());
    }

    private StackAndClusterUpscaleTriggerEvent fullUpscaleEvent(ClusterRepairTriggerEvent event, Map<String, Set<String>> groupsWithHostNames,
            boolean singlePrimaryGateway, boolean restartServices, boolean kerberosSecured) {
        Set<InstanceGroupView> instanceGroupViews = instanceGroupService.findViewByStackId(event.getStackId());
        boolean singleNodeCluster = isSingleNode(instanceGroupViews);
        Integer adjustmentSize = groupsWithHostNames.values().stream().map(Set::size).reduce(0, Integer::sum);
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) adjustmentSize);
        Map<String, Integer> hostGroupAdjustments = groupsWithHostNames.entrySet().stream().collect(Collectors.toMap(Entry::getKey, o -> o.getValue().size()));
        LOGGER.info("Full upscale with host groups and adjustments: {}", hostGroupAdjustments);
        LOGGER.info("Full upscale with host groups and host names: {}", groupsWithHostNames);
        return new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getResourceId(),
                hostGroupAdjustments, null, groupsWithHostNames, ScalingType.UPSCALE_TOGETHER, singlePrimaryGateway,
                kerberosSecured, event.accepted(), singleNodeCluster, restartServices, ClusterManagerType.CLOUDERA_MANAGER, adjustmentTypeWithThreshold,
                event.getTriggeredStackVariant()).setRepair();
    }

    public boolean isSingleNode(Set<InstanceGroupView> instanceGroupViews) {
        int nodeCount = 0;
        for (InstanceGroupView ig : instanceGroupViews) {
            nodeCount += ig.getNodeCount();
        }
        return nodeCount == 1;
    }

    private boolean isKerberosSecured(Long stackId) {
        StackView stack = stackViewService.getById(stackId);
        return kerberosConfigService.isKerberosConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName());
    }

    private static class RepairConfig {
        private Optional<Repair> singlePrimaryGateway;

        private List<Repair> repairs;

        RepairConfig() {
            singlePrimaryGateway = Optional.empty();
            repairs = new ArrayList<>();
        }

        public Optional<Repair> getSinglePrimaryGateway() {
            return singlePrimaryGateway;
        }

        public void setSinglePrimaryGateway(Repair singlePrimaryGateway) {
            this.singlePrimaryGateway = Optional.of(singlePrimaryGateway);
        }

        public List<Repair> getRepairs() {
            return repairs;
        }

        public void addRepairs(Repair repair) {
            repairs.add(repair);
        }
    }

    private static class Repair {

        private final String instanceGroupName;

        private final String hostGroupName;

        private final List<String> hostNames;

        Repair(String instanceGroupName, String hostGroupName, List<String> hostNames) {
            this.instanceGroupName = instanceGroupName;
            this.hostGroupName = hostGroupName;
            this.hostNames = hostNames;
        }

        public String getInstanceGroupName() {
            return instanceGroupName;
        }

        public String getHostGroupName() {
            return hostGroupName;
        }

        public List<String> getHostNames() {
            return hostNames;
        }
    }
}
