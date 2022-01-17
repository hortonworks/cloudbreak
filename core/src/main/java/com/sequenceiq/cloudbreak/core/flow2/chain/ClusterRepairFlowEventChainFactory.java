package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterRepairTriggerEvent event) {
        RepairConfig repairConfig = createRepairConfig(event);
        return new FlowTriggerEventQueue(getName(), event, createFlowTriggers(event, repairConfig));
    }

    private RepairConfig createRepairConfig(ClusterRepairTriggerEvent event) {
        RepairConfig repairConfig = new RepairConfig();
        StackView stack = stackViewService.getById(event.getStackId());
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

    private Queue<Selectable> createFlowTriggers(ClusterRepairTriggerEvent event, RepairConfig repairConfig) {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        flowTriggers.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        Map<String, Set<String>> repairableGroupsWithHostNames = new HashMap<>();
        boolean singlePrimaryGW = fillRepairableGroupsWithHostNames(repairConfig, repairableGroupsWithHostNames);
        LOGGER.info("Repairable groups with host names: {}", repairableGroupsWithHostNames);
        addDownscaleAndUpscaleEvents(event, flowTriggers, repairableGroupsWithHostNames, singlePrimaryGW);
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
            Set<String>> repairableGroupsWithHostNames, boolean singlePrimaryGW) {
        if (!repairableGroupsWithHostNames.isEmpty()) {
            flowTriggers.add(downscaleEvent(singlePrimaryGW, event, repairableGroupsWithHostNames));
            LOGGER.info("Downscale event added for: {}", repairableGroupsWithHostNames);
            for (Entry<String, Set<String>> groupWithHostNames : repairableGroupsWithHostNames.entrySet()) {
                addAwsNativeEventMigrationIfNeed(flowTriggers, event.getResourceId(), groupWithHostNames.getKey(), event.isUpgrade(),
                        event.getTriggeredStackVariant());
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

    void addAwsNativeEventMigrationIfNeed(Queue<Selectable> flowTriggers, Long resourceId, String groupName, boolean upgrade, String triggeredVariant) {
        if (upgrade) {
            LOGGER.debug("Upgrade flow, check the variant to migration, variant: {}, groupName: {}", triggeredVariant, groupName);
            if (AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value().equals(triggeredVariant)) {
                LOGGER.debug("Migration needed, variant: {}, groupName: {}", triggeredVariant, groupName);
                flowTriggers.add(awsVariantMigrationTriggerEvent(resourceId, groupName));
            }
        } else {
            LOGGER.debug("Don't need to migrate the stack, variant: {}, groupName: {}", triggeredVariant, groupName);
        }
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
