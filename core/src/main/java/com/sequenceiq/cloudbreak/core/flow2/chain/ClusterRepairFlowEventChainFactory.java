package com.sequenceiq.cloudbreak.core.flow2.chain;

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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClustersUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RescheduleStatusCheckTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class ClusterRepairFlowEventChainFactory implements FlowEventChainFactory<ClusterRepairTriggerEvent> {

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(ClusterRepairTriggerEvent event) {
        RepairConfig repairConfig = createRepairConfig(event);
        return createFlowTriggers(event, repairConfig);
    }

    private RepairConfig createRepairConfig(ClusterRepairTriggerEvent event) {
        RepairConfig repairConfig = new RepairConfig();
        Stack stack = stackService.getByIdWithListsInTransaction(event.getStackId());
        for (Entry<String, List<String>> failedNodes : event.getFailedNodesMap().entrySet()) {
            String hostGroupName = failedNodes.getKey();
            List<String> hostNames = failedNodes.getValue();
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupName)
                    .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
            InstanceGroup instanceGroup = hostGroup.getInstanceGroup();
            if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                Optional<String> primaryGatewayHostName = instanceMetaDataService.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(stack.getId(),
                        instanceGroup.getId());
                boolean primaryGatewayReparaiable = primaryGatewayHostName.isPresent() && hostNames.contains(primaryGatewayHostName.get());
                boolean singlePrimaryGatewayRepairable = primaryGatewayReparaiable && !stack.isMultipleGateway();
                if (singlePrimaryGatewayRepairable) {
                    repairConfig.setSinglePrimaryGateway(new Repair(instanceGroup.getGroupName(), hostGroup.getName(), hostNames));
                } else if (primaryGatewayReparaiable) {
                    repairConfig.setChangePGW(true);
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
        if (repairConfig.getSinglePrimaryGateway().isPresent()) {
            Repair repair = repairConfig.getSinglePrimaryGateway().get();
            flowTriggers.add(stackDownscaleEvent(event, repair.getHostGroupName(), repair.getHostNames()));
            flowTriggers.add(fullUpscaleEvent(event, repair.getHostGroupName(), repair.getHostNames(), true,
                    isKerberosSecured(event.getStackId())));
        } else if (repairConfig.isChangePGW()) {
            flowTriggers.add(changePrimaryGatewayEvent(event));
        }
        for (Repair repair : repairConfig.getRepairs()) {
            flowTriggers.add(fullDownscaleEvent(event, repair.getHostGroupName(), repair.getHostNames()));
            if (!event.isRemoveOnly()) {
                flowTriggers.add(fullUpscaleEvent(event, repair.getHostGroupName(), repair.getHostNames(), false, false));
            }
        }
        if (!event.isRemoveOnly() && (repairConfig.getSinglePrimaryGateway().isPresent() || repairConfig.isChangePGW())
                && !stackService.findClustersConnectedToDatalakeByDatalakeStackId(event.getResourceId()).isEmpty()) {
            flowTriggers.add(upgradeEphemeralClustersEvent(event));
        }
        flowTriggers.add(rescheduleStatusCheckEvent(event));
        return flowTriggers;
    }

    private StackDownscaleTriggerEvent stackDownscaleEvent(ClusterRepairTriggerEvent event, String groupName, List<String> hostNames) {
        Stack stack = stackService.getByIdWithListsInTransaction(event.getStackId());
        Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), new HashSet<>(hostNames));
        return new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), groupName, privateIdsForHostNames, event.accepted());
    }

    private ClusterAndStackDownscaleTriggerEvent fullDownscaleEvent(ClusterRepairTriggerEvent event, String hostGroupName, List<String> hostNames) {
        Stack stack = stackService.getByIdWithListsInTransaction(event.getStackId());
        Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), hostNames);
        return new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getResourceId(),
                hostGroupName, Sets.newHashSet(privateIdsForHostNames), ScalingType.DOWNSCALE_TOGETHER, event.accepted(),
                new ClusterDownscaleDetails(true, true));
    }

    private ChangePrimaryGatewayTriggerEvent changePrimaryGatewayEvent(ClusterRepairTriggerEvent event) {
        return new ChangePrimaryGatewayTriggerEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT.event(),
                event.getResourceId(), event.accepted());
    }

    private EphemeralClustersUpgradeTriggerEvent upgradeEphemeralClustersEvent(ClusterRepairTriggerEvent event) {
        return new EphemeralClustersUpgradeTriggerEvent(FlowChainTriggers.EPHEMERAL_CLUSTERS_UPDATE_TRIGGER_EVENT,
                event.getResourceId(), event.accepted());
    }

    private RescheduleStatusCheckTriggerEvent rescheduleStatusCheckEvent(ClusterRepairTriggerEvent event) {
        return new RescheduleStatusCheckTriggerEvent(FlowChainTriggers.RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT,
                event.getResourceId(), event.accepted());
    }

    private StackAndClusterUpscaleTriggerEvent fullUpscaleEvent(ClusterRepairTriggerEvent event, String hostGroupName, List<String> hostNames,
                                                                boolean singlePrimaryGateway, boolean kerberosSecured) {
        Stack stack = stackService.getByIdWithListsInTransaction(event.getStackId());
        boolean singleNodeCluster = clusterService.isSingleNode(stack);
        ClusterManagerType cmType = ClusterManagerType.CLOUDERA_MANAGER;
        return new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getResourceId(), hostGroupName,
                hostNames.size(), ScalingType.UPSCALE_TOGETHER, Sets.newHashSet(hostNames), singlePrimaryGateway,
                kerberosSecured, event.accepted(), singleNodeCluster, cmType).setRepair();
    }

    private boolean isKerberosSecured(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        return kerberosConfigService.isKerberosConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName());
    }

    private static class RepairConfig {

        private boolean changePGW;

        private Optional<Repair> singlePrimaryGateway;

        private List<Repair> repairs;

        RepairConfig() {
            singlePrimaryGateway = Optional.empty();
            repairs = new ArrayList<>();
        }

        public boolean isChangePGW() {
            return changePGW;
        }

        public void setChangePGW(boolean changePGW) {
            this.changePGW = changePGW;
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
