package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayTriggerEvent;
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
        if (repairConfig.getSinglePrimaryGateway().isPresent()) {
            Repair repair = repairConfig.getSinglePrimaryGateway().get();
            flowTriggers.add(stackDownscaleEvent(event, repair.getHostGroupName(), repair.getHostNames()));
            flowTriggers.add(fullUpscaleEvent(event, repair.getHostGroupName(), repair.getHostNames(), true,
                    event.isRestartServices(), isKerberosSecured(event.getStackId())));
        }
        for (Repair repair : repairConfig.getRepairs()) {
            flowTriggers.add(fullDownscaleEvent(event, repair.getHostGroupName(), repair.getHostNames()));
            if (!event.isRemoveOnly()) {
                flowTriggers.add(fullUpscaleEvent(event, repair.getHostGroupName(), repair.getHostNames(), false,
                        event.isRestartServices(), false));
            }
        }
        flowTriggers.add(rescheduleStatusCheckEvent(event));
        flowTriggers.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return flowTriggers;
    }

    private StackDownscaleTriggerEvent stackDownscaleEvent(ClusterRepairTriggerEvent event, String groupName, List<String> hostNames) {
        Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.getAllInstanceMetadataWithoutInstanceGroupByStackId(event.getStackId());
        Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(instanceMetaData, new HashSet<>(hostNames));
        return new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), groupName, privateIdsForHostNames, event.accepted())
                .setRepair();
    }

    private ClusterAndStackDownscaleTriggerEvent fullDownscaleEvent(ClusterRepairTriggerEvent event, String hostGroupName, List<String> hostNames) {
        Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.getAllInstanceMetadataWithoutInstanceGroupByStackId(event.getStackId());
        Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(instanceMetaData, hostNames);
        return new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getResourceId(),
                hostGroupName, Sets.newHashSet(privateIdsForHostNames), ScalingType.DOWNSCALE_TOGETHER, event.accepted(),
                new ClusterDownscaleDetails(true, true));
    }

    private ChangePrimaryGatewayTriggerEvent changePrimaryGatewayEvent(ClusterRepairTriggerEvent event) {
        return new ChangePrimaryGatewayTriggerEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT.event(),
                event.getResourceId(), event.accepted());
    }

    private RescheduleStatusCheckTriggerEvent rescheduleStatusCheckEvent(ClusterRepairTriggerEvent event) {
        return new RescheduleStatusCheckTriggerEvent(FlowChainTriggers.RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT,
                event.getResourceId(), event.accepted());
    }

    private StackAndClusterUpscaleTriggerEvent fullUpscaleEvent(ClusterRepairTriggerEvent event, String hostGroupName, List<String> hostNames,
            boolean singlePrimaryGateway, boolean restartServices, boolean kerberosSecured) {
        Set<InstanceGroupView> instanceGroupViews = instanceGroupService.findViewByStackId(event.getStackId());
        boolean singleNodeCluster = isSingleNode(instanceGroupViews);
        ClusterManagerType cmType = ClusterManagerType.CLOUDERA_MANAGER;
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) hostNames.size());
        return new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getResourceId(), hostGroupName,
                hostNames.size(), ScalingType.UPSCALE_TOGETHER, Sets.newHashSet(hostNames), singlePrimaryGateway,
                kerberosSecured, event.accepted(), singleNodeCluster, restartServices, cmType, adjustmentTypeWithThreshold).setRepair();
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
